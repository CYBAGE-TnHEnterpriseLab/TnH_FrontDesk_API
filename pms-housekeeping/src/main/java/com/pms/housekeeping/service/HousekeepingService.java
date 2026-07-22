package com.pms.housekeeping.service;

import com.pms.housekeeping.common.exception.HousekeepingNotFoundException;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.AssignableRoomResponse;
import com.pms.housekeeping.dto.response.HousekeepingDashboardResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomRowResponse;
import com.pms.housekeeping.dto.response.HousekeepingRoomsPageResponse;
import com.pms.housekeeping.dto.response.HousekeepingStatusUpdateResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusHistoryRepository;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusRepository;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HousekeepingService {

    private final HousekeepingRoomDayStatusRepository dayStatusRepository;
    private final HousekeepingRoomDayStatusHistoryRepository historyRepository;
    private final RoomMasterProjectionRepository roomMasterProjectionRepository;

    public HousekeepingService(
            HousekeepingRoomDayStatusRepository dayStatusRepository,
            HousekeepingRoomDayStatusHistoryRepository historyRepository,
            RoomMasterProjectionRepository roomMasterProjectionRepository
    ) {
        this.dayStatusRepository = dayStatusRepository;
        this.historyRepository = historyRepository;
        this.roomMasterProjectionRepository = roomMasterProjectionRepository;
    }

    @Transactional(readOnly = true)
    public HousekeepingDashboardResponse dashboard(UUID propertyId, LocalDate businessDate) {
        List<HousekeepingRoomDayStatus> rows = dayStatusRepository.findAllByPropertyIdAndBusinessDate(propertyId, businessDate);

        long totalRooms = rows.size();
        long vacantClean = count(rows, r -> r.getFrontOfficeStatus() == FrontOfficeStatus.VACANT && r.getCleaningStatus() == CleaningStatus.CLEAN);
        long vacantDirty = count(rows, r -> r.getFrontOfficeStatus() == FrontOfficeStatus.VACANT && r.getCleaningStatus() == CleaningStatus.DIRTY);
        long occupiedClean = count(rows, r -> r.getFrontOfficeStatus() == FrontOfficeStatus.OCCUPIED && r.getCleaningStatus() == CleaningStatus.CLEAN);
        long occupiedDirty = count(rows, r -> r.getFrontOfficeStatus() == FrontOfficeStatus.OCCUPIED && r.getCleaningStatus() == CleaningStatus.DIRTY);
        long outOfOrder = count(rows, r -> r.getCleaningStatus() == CleaningStatus.OUT_OF_ORDER);
        long outOfService = count(rows, r -> r.getCleaningStatus() == CleaningStatus.OUT_OF_SERVICE);
        long inspected = count(rows, r -> r.getCleaningStatus() == CleaningStatus.INSPECTED);
        long pickup = count(rows, r -> r.getCleaningStatus() == CleaningStatus.PICKUP);
        long arrivals = count(rows, r -> r.getReservationStatus() == ReservationStatus.ARRIVAL);
        long departures = count(rows, r -> r.getReservationStatus() == ReservationStatus.DEPARTURE);

        return new HousekeepingDashboardResponse(
                totalRooms,
                vacantClean,
                vacantDirty,
                occupiedClean,
                occupiedDirty,
                outOfOrder,
                outOfService,
                inspected,
                pickup,
                arrivals,
                departures
        );
    }

    @Transactional(readOnly = true)
    public HousekeepingRoomsPageResponse rooms(
            UUID propertyId,
            LocalDate businessDate,
            List<CleaningStatus> cleaningStatuses,
            List<FrontOfficeStatus> frontOfficeStatuses,
            List<ReservationStatus> reservationStatuses,
            UUID roomTypeId,
            String floor,
            String zone,
            String roomClass,
            String attendant,
            Boolean vipOnly,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        List<HousekeepingRoomDayStatus> rows = dayStatusRepository.findAllByPropertyIdAndBusinessDate(propertyId, businessDate);
        Map<String, RoomMasterProjection> roomMasterByNumber = roomMasterProjectionRepository.findAllByPropertyId(propertyId)
                .stream()
                .collect(Collectors.toMap(RoomMasterProjection::getRoomNumber, projection -> projection, (left, right) -> left, HashMap::new));

        List<HousekeepingRoomRowResponse> filtered = rows.stream()
                .map(row -> toRowResponse(row, roomMasterByNumber.get(row.getRoomNumber())))
                .filter(row -> roomTypeId == null || roomTypeId.equals(row.roomTypeId()))
                .filter(row -> cleaningStatuses == null || cleaningStatuses.isEmpty() || cleaningStatuses.contains(CleaningStatus.valueOf(row.cleaningStatus())))
                .filter(row -> frontOfficeStatuses == null || frontOfficeStatuses.isEmpty() || frontOfficeStatuses.contains(FrontOfficeStatus.valueOf(row.frontOfficeStatus())))
                .filter(row -> reservationStatuses == null || reservationStatuses.isEmpty() || reservationStatuses.contains(ReservationStatus.valueOf(row.reservationStatus())))
                .filter(row -> isBlank(floor) || equalsIgnoreCase(floor, row.floor()))
                .filter(row -> isBlank(zone) || equalsIgnoreCase(zone, row.zone()))
                .filter(row -> isBlank(roomClass) || equalsIgnoreCase(roomClass, row.roomClass()))
                .filter(row -> isBlank(attendant) || equalsIgnoreCase(attendant, row.attendantName()))
                .filter(row -> vipOnly == null || !vipOnly || "VIP".equalsIgnoreCase(row.priority()))
                .sorted(buildComparator(sortBy, sortDir))
                .toList();

        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        int totalPages = (int) Math.ceil((double) filtered.size() / safeSize);

        return new HousekeepingRoomsPageResponse(
                safePage,
                safeSize,
                filtered.size(),
                totalPages,
                filtered.subList(from, to)
        );
    }

    @Transactional(readOnly = true)
    public List<AssignableRoomResponse> assignableRooms(UUID propertyId, LocalDate businessDate, UUID roomTypeId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<HousekeepingRoomDayStatus> rows = dayStatusRepository
                .findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndAssignedReservationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
                        propertyId,
                        businessDate,
                        roomTypeId,
                        List.of(CleaningStatus.CLEAN, CleaningStatus.INSPECTED),
                        FrontOfficeStatus.VACANT
                );

        Map<String, RoomMasterProjection> roomMasterByNumber = roomMasterProjectionRepository.findAllByPropertyId(propertyId)
                .stream()
                .collect(Collectors.toMap(RoomMasterProjection::getRoomNumber, projection -> projection, (left, right) -> left, HashMap::new));

        List<AssignableRoomResponse> out = new ArrayList<>();
        for (HousekeepingRoomDayStatus row : rows) {
            RoomMasterProjection projection = roomMasterByNumber.get(row.getRoomNumber());
            out.add(new AssignableRoomResponse(
                    row.getRoomNumber(),
                    row.getRoomTypeId(),
                    projection == null ? null : projection.getRoomTypeName(),
                    projection == null ? null : projection.getFloor(),
                    projection == null ? null : projection.getRoomClass(),
                    projection == null ? null : projection.getZone(),
                    row.getCleaningStatus().name()
            ));
            if (out.size() >= safeLimit) {
                break;
            }
        }

        return out;
    }

    @Transactional
    public HousekeepingStatusUpdateResponse updateRoomStatus(
            String roomNumber,
            UpdateHousekeepingStatusRequest request
    ) {
        HousekeepingRoomDayStatus row = dayStatusRepository
                .findByPropertyIdAndBusinessDateAndRoomNumber(request.propertyId(), request.businessDate(), roomNumber)
                .orElseThrow(() -> new HousekeepingNotFoundException("Housekeeping status not found for room: " + roomNumber));

        LocalDateTime now = LocalDateTime.now();

        applyCleaningStatusChange(row, request, now);
        applyFrontOfficeStatusChange(row, request, now);
        applyReservationStatusChange(row, request, now);

        if (request.assignedReservationId() != null || row.getAssignedReservationId() != null) {
            UUID oldValue = row.getAssignedReservationId();
            UUID newValue = request.assignedReservationId();
            if (!Objects.equals(oldValue, newValue)) {
                row.setAssignedReservationId(newValue);
                saveHistory(row, "assignedReservationId", toStringValue(oldValue), toStringValue(newValue), request, now);
            }
        }

        if (request.attendantName() != null && !Objects.equals(row.getAttendantName(), request.attendantName())) {
            saveHistory(row, "attendantName", row.getAttendantName(), request.attendantName(), request, now);
            row.setAttendantName(request.attendantName());
        }

        if (request.priority() != null && request.priority() != row.getPriority()) {
            saveHistory(row, "priority", row.getPriority().name(), request.priority().name(), request, now);
            row.setPriority(request.priority());
        }

        if (request.guestDisplayName() != null) {
            row.setGuestDisplayName(request.guestDisplayName());
        }
        if (request.arrivalDate() != null) {
            row.setArrivalDate(request.arrivalDate());
        }
        if (request.departureDate() != null) {
            row.setDepartureDate(request.departureDate());
        }

        row.setSellable(request.sellable() != null ? request.sellable() : computeSellable(row));
        row.setUpdatedBy(request.updatedBy());
        row.setUpdatedAt(now);

        HousekeepingRoomDayStatus saved = dayStatusRepository.save(row);
        return new HousekeepingStatusUpdateResponse(
                saved.getPropertyId(),
                saved.getBusinessDate(),
                saved.getRoomNumber(),
                saved.getCleaningStatus().name(),
                saved.getFrontOfficeStatus().name(),
                saved.getReservationStatus().name(),
                saved.getAssignedReservationId(),
                saved.isSellable(),
                saved.getUpdatedAt()
        );
    }

    private void applyCleaningStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now) {
        if (request.cleaningStatus() == null || request.cleaningStatus() == row.getCleaningStatus()) {
            return;
        }
        saveHistory(row, "cleaningStatus", row.getCleaningStatus().name(), request.cleaningStatus().name(), request, now);
        row.setCleaningStatus(request.cleaningStatus());
        row.setStatusChangedAt(now);
        if (request.cleaningStatus() == CleaningStatus.CLEAN || request.cleaningStatus() == CleaningStatus.INSPECTED) {
            row.setLastCleanedAt(now);
        }
    }

    private void applyFrontOfficeStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now) {
        if (request.frontOfficeStatus() == null || request.frontOfficeStatus() == row.getFrontOfficeStatus()) {
            return;
        }
        saveHistory(row, "frontOfficeStatus", row.getFrontOfficeStatus().name(), request.frontOfficeStatus().name(), request, now);
        row.setFrontOfficeStatus(request.frontOfficeStatus());
        row.setFoStatusChangedAt(now);
    }

    private void applyReservationStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now) {
        if (request.reservationStatus() == null || request.reservationStatus() == row.getReservationStatus()) {
            return;
        }
        saveHistory(row, "reservationStatus", row.getReservationStatus().name(), request.reservationStatus().name(), request, now);
        row.setReservationStatus(request.reservationStatus());
        row.setReservationStatusChangedAt(now);
    }

    private void saveHistory(
            HousekeepingRoomDayStatus row,
            String field,
            String oldValue,
            String newValue,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now
    ) {
        HousekeepingRoomDayStatusHistory history = HousekeepingRoomDayStatusHistory.builder()
                .propertyId(row.getPropertyId())
                .businessDate(row.getBusinessDate())
                .roomNumber(row.getRoomNumber())
                .changedField(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(now)
                .changedBy(request.updatedBy())
                .sourceModule(request.sourceModule())
                .reason(request.reason())
                .build();
        historyRepository.save(history);
    }

    private long count(List<HousekeepingRoomDayStatus> rows, java.util.function.Predicate<HousekeepingRoomDayStatus> predicate) {
        return rows.stream().filter(predicate).count();
    }

    private HousekeepingRoomRowResponse toRowResponse(HousekeepingRoomDayStatus status, RoomMasterProjection roomMaster) {
        return new HousekeepingRoomRowResponse(
                status.getRoomNumber(),
                status.getRoomTypeId(),
                roomMaster == null ? null : roomMaster.getRoomTypeName(),
                status.getCleaningStatus().name(),
                status.getFrontOfficeStatus().name(),
                status.getReservationStatus().name(),
                status.getGuestDisplayName(),
                status.getArrivalDate(),
                status.getDepartureDate(),
                roomMaster == null ? null : roomMaster.getFloor(),
                roomMaster == null ? null : roomMaster.getRoomClass(),
                roomMaster == null ? null : roomMaster.getZone(),
                status.getAttendantName(),
                status.getLastCleanedAt(),
                status.getPriority().name(),
                roomMaster == null ? null : roomMaster.getFeaturesCsv(),
                status.isSellable(),
                status.getAssignedReservationId()
        );
    }

    private Comparator<HousekeepingRoomRowResponse> buildComparator(String sortBy, String sortDir) {
        String normalizedSort = Optional.ofNullable(sortBy).orElse("roomNumber");
        Comparator<HousekeepingRoomRowResponse> comparator;
        switch (normalizedSort) {
            case "priority" -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::priority, this::compareNullableText);
            case "arrivalDate" -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::arrivalDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "departureDate" -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::departureDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "lastCleanedAt" -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::lastCleanedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "roomTypeName" -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::roomTypeName, this::compareNullableText);
            default -> comparator = Comparator.comparing(HousekeepingRoomRowResponse::roomNumber, this::compareNullableText);
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private int compareNullableText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
    }

    private boolean computeSellable(HousekeepingRoomDayStatus status) {
        boolean cleanEnough = status.getCleaningStatus() == CleaningStatus.CLEAN
                || status.getCleaningStatus() == CleaningStatus.INSPECTED;
        boolean vacant = status.getFrontOfficeStatus() == FrontOfficeStatus.VACANT;
        boolean noAssignment = status.getAssignedReservationId() == null;
        boolean notOut = status.getCleaningStatus() != CleaningStatus.OUT_OF_ORDER
                && status.getCleaningStatus() != CleaningStatus.OUT_OF_SERVICE;
        return cleanEnough && vacant && noAssignment && notOut;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}


