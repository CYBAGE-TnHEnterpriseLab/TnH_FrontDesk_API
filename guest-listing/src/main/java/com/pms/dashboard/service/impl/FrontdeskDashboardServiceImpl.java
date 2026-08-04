package com.pms.dashboard.service.impl;

import com.pms.dashboard.client.HousekeepingDashboardClient;
import com.pms.dashboard.client.InventoryDashboardClient;
import com.pms.dashboard.client.PropertyDashboardClient;
import com.pms.dashboard.client.RateDashboardClient;
import com.pms.dashboard.client.ReservationDashboardClient;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.dto.response.FrontdeskDashboardResponse;
import com.pms.dashboard.service.FrontdeskDashboardService;
import com.pms.dashboard.service.SourceResult;
import com.pms.dashboard.service.model.DashboardModels;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FrontdeskDashboardServiceImpl implements FrontdeskDashboardService {

    private static final Logger log = LoggerFactory.getLogger(FrontdeskDashboardServiceImpl.class);

    private final HousekeepingDashboardClient housekeepingClient;
    private final InventoryDashboardClient inventoryClient;
    private final PropertyDashboardClient propertyClient;
    private final RateDashboardClient rateClient;
    private final ReservationDashboardClient reservationClient;
    private final DashboardProperties properties;

    public FrontdeskDashboardServiceImpl(
            HousekeepingDashboardClient housekeepingClient,
            InventoryDashboardClient inventoryClient,
            PropertyDashboardClient propertyClient,
            RateDashboardClient rateClient,
            ReservationDashboardClient reservationClient,
            DashboardProperties properties
    ) {
        this.housekeepingClient = housekeepingClient;
        this.inventoryClient = inventoryClient;
        this.propertyClient = propertyClient;
        this.rateClient = rateClient;
        this.reservationClient = reservationClient;
        this.properties = properties;
    }

    @Override
    public FrontdeskDashboardResponse getDashboard(UUID propertyId, LocalDate businessDate) {
        Duration timeout = Duration.ofMillis(Math.max(500, properties.getTimeoutMs()));

        Mono<SourceResult<DashboardModels.HousekeepingDashboardData>> housekeepingSummary = wrap(
                "housekeepingSummary",
                housekeepingClient.fetchDashboard(propertyId, businessDate),
                DashboardModels.HousekeepingDashboardData.empty(),
                timeout
        ).cache();

        Mono<SourceResult<List<DashboardModels.HousekeepingRoomData>>> housekeepingRoomsToday = wrap(
                "housekeepingRoomsToday",
                housekeepingClient.fetchRooms(propertyId, businessDate),
                List.of(),
                timeout
        ).cache();

        Mono<SourceResult<List<DashboardModels.HousekeepingRoomData>>> housekeepingRoomsTomorrow = wrap(
                "housekeepingRoomsTomorrow",
                housekeepingClient.fetchRooms(propertyId, businessDate.plusDays(1)),
                List.of(),
                timeout
        ).cache();

        Mono<SourceResult<List<DashboardModels.PropertyRoomTypeData>>> propertyRoomTypes = wrap(
                "propertyRoomTypes",
                propertyClient.fetchRoomTypes(propertyId),
                List.of(),
                timeout
        ).cache();

        Mono<SourceResult<List<DashboardModels.RatePlanData>>> ratePlans = wrap(
                "ratePlans",
                rateClient.fetchRatePlans(propertyId),
                List.of(),
                timeout
        );

        Mono<SourceResult<DashboardModels.ReservationFlowData>> reservationFlow = wrap(
                "reservationFlow",
                reservationClient.fetchFlow(propertyId, businessDate),
                DashboardModels.ReservationFlowData.empty(),
                timeout
        );

        Mono<SourceResult<List<FrontdeskDashboardResponse.RoomTypeOverview>>> roomTypeOverview =
                Mono.zip(propertyRoomTypes, housekeepingRoomsToday)
                        .flatMap(tuple -> {
                            SourceResult<List<DashboardModels.PropertyRoomTypeData>> propertyTypes = tuple.getT1();
                            SourceResult<List<DashboardModels.HousekeepingRoomData>> hkRooms = tuple.getT2();

                            List<DashboardModels.PropertyRoomTypeData> usable = propertyTypes.payload().stream()
                                    .filter(type -> type.roomTypeId() != null)
                                    .toList();

                            if (!usable.isEmpty()) {
                                return fetchRoomTypeOverview(propertyId, businessDate, usable, propertyTypes.status(), timeout);
                            }

                            List<DashboardModels.PropertyRoomTypeData> fallbackTypes = deriveRoomTypesFromHousekeeping(hkRooms.payload());
                            return fetchRoomTypeOverview(propertyId, businessDate, fallbackTypes, "DEGRADED", timeout);
                        });

        Mono<FrontdeskDashboardResponse> merged = Mono.zip(
                housekeepingSummary,
                housekeepingRoomsToday,
                housekeepingRoomsTomorrow,
                propertyRoomTypes,
                ratePlans,
                reservationFlow,
                roomTypeOverview
        ).map(tuple -> {
            SourceResult<DashboardModels.HousekeepingDashboardData> hkSummary = tuple.getT1();
            SourceResult<List<DashboardModels.HousekeepingRoomData>> hkToday = tuple.getT2();
            SourceResult<List<DashboardModels.HousekeepingRoomData>> hkTomorrow = tuple.getT3();
            SourceResult<List<DashboardModels.PropertyRoomTypeData>> roomTypes = tuple.getT4();
            SourceResult<List<DashboardModels.RatePlanData>> rate = tuple.getT5();
            SourceResult<DashboardModels.ReservationFlowData> reservation = tuple.getT6();
            SourceResult<List<FrontdeskDashboardResponse.RoomTypeOverview>> roomOverview = tuple.getT7();

            List<FrontdeskDashboardResponse.RoomTypeOverview> resolvedRoomOverview = roomOverview.payload();
            if (isZeroedRoomOverview(resolvedRoomOverview)) {
                List<FrontdeskDashboardResponse.RoomTypeOverview> fallbackOverview = deriveRoomTypeOverviewFromHousekeeping(hkToday.payload());
                if (!fallbackOverview.isEmpty()) {
                    resolvedRoomOverview = fallbackOverview;
                    log.warn("Room type overview fallback used from housekeeping rooms for propertyId={} businessDate={}", propertyId, businessDate);
                }
            }

            FrontdeskDashboardResponse.RoomInventoryMetrics inventoryMetrics = summarizeInventory(
                    hkSummary.payload(),
                    hkToday.payload(),
                    resolvedRoomOverview
            );

            long availableTonight = resolvedRoomOverview.stream().mapToLong(FrontdeskDashboardResponse.RoomTypeOverview::available).sum();
            if (availableTonight == 0L) {
                availableTonight = hkSummary.payload().vacantClean() + hkSummary.payload().inspected();
            }

            long occupiedTonight = hkSummary.payload().occupiedClean() + hkSummary.payload().occupiedDirty();
            double occupancyPercent = toPercent(occupiedTonight, Math.max(inventoryMetrics.totalRooms(), hkSummary.payload().totalRooms()));

            long arrivals = Math.max(hkSummary.payload().arrivals(), reservation.payload().arrivals());
            long departures = Math.max(hkSummary.payload().departures(), reservation.payload().departures());
            long stayovers = Math.max(occupiedTonight - arrivals, 0);

            FrontdeskDashboardResponse.HousekeepingRoomStatus housekeepingStatus = summarizeHousekeepingStatus(hkSummary.payload(), hkToday.payload());

            Map<String, String> sources = new LinkedHashMap<>();
            sources.put("housekeepingSummary", hkSummary.status());
            sources.put("housekeepingRoomsToday", hkToday.status());
            sources.put("housekeepingRoomsTomorrow", hkTomorrow.status());
            sources.put("propertyRoomTypes", roomTypes.status());
            sources.put("inventory", roomOverview.status());
            sources.put("ratePlans", rate.status());
            sources.put("reservationFlow", reservation.status());

            return new FrontdeskDashboardResponse(
                    propertyId,
                    businessDate,
                    new FrontdeskDashboardResponse.Kpis(availableTonight, occupiedTonight, occupancyPercent),
                    new FrontdeskDashboardResponse.ComplimentaryHouseUse(arrivals, arrivals, stayovers, stayovers, departures, departures),
                    summarizeRevenue(rate.payload()),
                    inventoryMetrics,
                    housekeepingStatus,
                    resolvedRoomOverview,
                    summarizeTomorrowStatus(hkTomorrow.payload()),
                    summarizeGuestActivity(arrivals, departures, stayovers, occupiedTonight),
                    sources
            );
        });

        return merged.blockOptional(timeout)
                .orElseThrow(() -> new IllegalStateException("Unable to build frontdesk dashboard response"));
    }

    private Mono<SourceResult<List<FrontdeskDashboardResponse.RoomTypeOverview>>> fetchRoomTypeOverview(
            UUID propertyId,
            LocalDate businessDate,
            List<DashboardModels.PropertyRoomTypeData> roomTypes,
            String sourceStatus,
            Duration timeout
    ) {
        if (roomTypes == null || roomTypes.isEmpty()) {
            return Mono.just(SourceResult.degraded(List.of()));
        }

        return Flux.fromIterable(roomTypes)
                .flatMap(type -> inventoryClient.fetchDaily(propertyId, type.roomTypeId(), businessDate)
                        .timeout(timeout)
                        .map(daily -> toRoomTypeOverview(type, daily))
                        .onErrorResume(ex -> {
                            log.warn("Inventory daily lookup failed for roomTypeId={}: {}", type.roomTypeId(), ex.getMessage());
                            return Mono.just(new FrontdeskDashboardResponse.RoomTypeOverview(
                                    coalesceTypeName(type.roomTypeCode(), type.roomTypeName()), 0, 0, 0
                            ));
                        }))
                .collectList()
                .map(items -> "OK".equals(sourceStatus) ? SourceResult.ok(items) : SourceResult.degraded(items));
    }

    private List<DashboardModels.PropertyRoomTypeData> deriveRoomTypesFromHousekeeping(List<DashboardModels.HousekeepingRoomData> rooms) {
        Map<UUID, DashboardModels.PropertyRoomTypeData> uniqueById = new LinkedHashMap<>();
        for (DashboardModels.HousekeepingRoomData room : rooms) {
            if (room.roomTypeId() == null || uniqueById.containsKey(room.roomTypeId())) {
                continue;
            }
            uniqueById.put(room.roomTypeId(), new DashboardModels.PropertyRoomTypeData(room.roomTypeId(), null, room.roomTypeName()));
        }
        return List.copyOf(uniqueById.values());
    }

    private FrontdeskDashboardResponse.RoomTypeOverview toRoomTypeOverview(
            DashboardModels.PropertyRoomTypeData type,
            DashboardModels.InventoryDailyData daily
    ) {
        long total = Math.max(daily.totalInventory(), 0);
        long booked = Math.max(daily.reservedCount(), 0) + Math.max(daily.blockedCount(), 0);
        long available = Math.max(daily.availableCount(), 0);
        return new FrontdeskDashboardResponse.RoomTypeOverview(coalesceTypeName(type.roomTypeCode(), type.roomTypeName()), total, booked, available);
    }

    private boolean isZeroedRoomOverview(List<FrontdeskDashboardResponse.RoomTypeOverview> roomTypeOverview) {
        if (roomTypeOverview == null || roomTypeOverview.isEmpty()) {
            return true;
        }
        return roomTypeOverview.stream().allMatch(item -> item.total() == 0 && item.booked() == 0 && item.available() == 0);
    }

    private List<FrontdeskDashboardResponse.RoomTypeOverview> deriveRoomTypeOverviewFromHousekeeping(List<DashboardModels.HousekeepingRoomData> rooms) {
        class Acc {
            long total;
            long booked;
            long available;
        }
        Map<String, Acc> grouped = new LinkedHashMap<>();
        for (DashboardModels.HousekeepingRoomData room : rooms) {
            String type = coalesceTypeName(null, room.roomTypeName());
            Acc acc = grouped.computeIfAbsent(type, ignored -> new Acc());
            acc.total++;
            if (containsAny(room.frontOfficeStatus(), "OCCUPIED")) {
                acc.booked++;
            }
            if (room.sellable()) {
                acc.available++;
            }
        }
        List<FrontdeskDashboardResponse.RoomTypeOverview> out = new ArrayList<>();
        for (Map.Entry<String, Acc> entry : grouped.entrySet()) {
            out.add(new FrontdeskDashboardResponse.RoomTypeOverview(entry.getKey(), entry.getValue().total, entry.getValue().booked, entry.getValue().available));
        }
        return out;
    }

    private FrontdeskDashboardResponse.RoomInventoryMetrics summarizeInventory(
            DashboardModels.HousekeepingDashboardData housekeeping,
            List<DashboardModels.HousekeepingRoomData> rooms,
            List<FrontdeskDashboardResponse.RoomTypeOverview> roomTypeOverview
    ) {
        long totalByType = roomTypeOverview.stream().mapToLong(FrontdeskDashboardResponse.RoomTypeOverview::total).sum();
        long totalRooms = totalByType > 0 ? totalByType : housekeeping.totalRooms();
        long sellable = rooms.stream().filter(DashboardModels.HousekeepingRoomData::sellable).count();
        if (sellable == 0) {
            sellable = Math.max(housekeeping.vacantClean() + housekeeping.inspected(), 0);
        }
        return new FrontdeskDashboardResponse.RoomInventoryMetrics(totalRooms, sellable, housekeeping.outOfOrder(), housekeeping.outOfService());
    }

    private FrontdeskDashboardResponse.HousekeepingRoomStatus summarizeHousekeepingStatus(
            DashboardModels.HousekeepingDashboardData summary,
            List<DashboardModels.HousekeepingRoomData> rooms
    ) {
        if (rooms == null || rooms.isEmpty()) {
            return new FrontdeskDashboardResponse.HousekeepingRoomStatus(
                    new FrontdeskDashboardResponse.Vacant(summary.inspected(), summary.vacantClean(), summary.vacantDirty(), summary.pickup()),
                    new FrontdeskDashboardResponse.Occupied(summary.occupiedClean(), 0, summary.occupiedDirty())
            );
        }

        long inspectedVacant = countRooms(rooms, "VACANT", "INSPECTED");
        long cleanVacant = countRooms(rooms, "VACANT", "CLEAN");
        long dirtyVacant = countRooms(rooms, "VACANT", "DIRTY");
        long pickupVacant = countRooms(rooms, "VACANT", "PICKUP");
        long cleanOccupied = countRooms(rooms, "OCCUPIED", "CLEAN") + countRooms(rooms, "OCCUPIED", "INSPECTED");
        long pickupOccupied = countRooms(rooms, "OCCUPIED", "PICKUP");
        long dirtyOccupied = countRooms(rooms, "OCCUPIED", "DIRTY");

        return new FrontdeskDashboardResponse.HousekeepingRoomStatus(
                new FrontdeskDashboardResponse.Vacant(inspectedVacant, cleanVacant, dirtyVacant, pickupVacant),
                new FrontdeskDashboardResponse.Occupied(cleanOccupied, pickupOccupied, dirtyOccupied)
        );
    }

    private FrontdeskDashboardResponse.TomorrowStatus summarizeTomorrowStatus(List<DashboardModels.HousekeepingRoomData> tomorrowRooms) {
        long required = tomorrowRooms.stream().filter(r -> containsAny(r.cleaningStatus(), "DIRTY", "PICKUP")).count();
        long notRequired = tomorrowRooms.stream().filter(r -> containsAny(r.cleaningStatus(), "OUT_OF_ORDER", "OUT_OF_SERVICE")).count();
        long completed = tomorrowRooms.stream().filter(r -> containsAny(r.cleaningStatus(), "CLEAN", "INSPECTED")).count();
        return new FrontdeskDashboardResponse.TomorrowStatus(required, notRequired, completed);
    }

    private FrontdeskDashboardResponse.RevenueMetrics summarizeRevenue(List<DashboardModels.RatePlanData> ratePlans) {
        if (ratePlans == null || ratePlans.isEmpty()) {
            return new FrontdeskDashboardResponse.RevenueMetrics(BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }
        BigDecimal roomRevenue = BigDecimal.ZERO;
        long group = 0;
        for (DashboardModels.RatePlanData ratePlan : ratePlans) {
            if (ratePlan.manualAmount() != null) {
                roomRevenue = roomRevenue.add(ratePlan.manualAmount());
            }
            if (containsAny(ratePlan.name(), "GROUP")) {
                group++;
            }
        }
        long individual = Math.max(ratePlans.size() - group, 0);
        BigDecimal average = roomRevenue.divide(BigDecimal.valueOf(ratePlans.size()), 2, RoundingMode.HALF_UP);
        return new FrontdeskDashboardResponse.RevenueMetrics(roomRevenue, average, individual, group);
    }

    private FrontdeskDashboardResponse.DailyGuestActivity summarizeGuestActivity(long arrivals, long departures, long stayovers, long occupiedTonight) {
        long checkedIn = Math.max(occupiedTonight - stayovers, 0);
        return new FrontdeskDashboardResponse.DailyGuestActivity(
                new FrontdeskDashboardResponse.Today(arrivals, checkedIn, 0, 0),
                new FrontdeskDashboardResponse.Arrivals(arrivals, departures, 0),
                new FrontdeskDashboardResponse.OtherActivity(stayovers, 0, 0, 0)
        );
    }

    private long countRooms(List<DashboardModels.HousekeepingRoomData> rooms, String frontOfficeStatus, String cleaningStatus) {
        return rooms.stream().filter(room -> containsAny(room.frontOfficeStatus(), frontOfficeStatus) && containsAny(room.cleaningStatus(), cleaningStatus)).count();
    }

    private boolean containsAny(String value, String... candidates) {
        if (value == null || candidates == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate != null && normalized.equals(candidate.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String coalesceTypeName(String code, String name) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (code != null && !code.isBlank()) {
            return code;
        }
        return "Unknown";
    }

    private double toPercent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private <T> Mono<SourceResult<T>> wrap(String source, Mono<T> mono, T fallback, Duration timeout) {
        return mono.timeout(timeout)
                .map(SourceResult::ok)
                .onErrorResume(ex -> {
                    log.warn("Dashboard source degraded: source={}, reason={}", source, ex.getMessage());
                    return Mono.just(SourceResult.degraded(fallback));
                });
    }
}

