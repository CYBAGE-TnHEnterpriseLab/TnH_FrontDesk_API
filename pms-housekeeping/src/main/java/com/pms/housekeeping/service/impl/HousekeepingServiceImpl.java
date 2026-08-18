package com.pms.housekeeping.service.impl;

import com.pms.housekeeping.common.exception.HousekeepingNotFoundException;
import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.*;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatusHistory;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.entity.RoomMasterProjection;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusHistoryRepository;
import com.pms.housekeeping.repository.HousekeepingRoomDayStatusRepository;
import com.pms.housekeeping.repository.RoomMasterProjectionRepository;
import com.pms.housekeeping.security.CurrentUserProvider;
import com.pms.housekeeping.service.HousekeepingService;
import com.pms.housekeeping.specifications.HousekeepingRoomSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
public class HousekeepingServiceImpl implements HousekeepingService {

    private static final Logger log = LoggerFactory.getLogger(HousekeepingServiceImpl.class);
    private final HousekeepingRoomDayStatusRepository dayStatusRepository;
    private final HousekeepingRoomDayStatusHistoryRepository historyRepository;
    private final RoomMasterProjectionRepository roomMasterProjectionRepository;
    private final HousekeepingRoomDayStatusRepository housekeepingRoomDayStatusRepository;
    private final CurrentUserProvider currentUserProvider;

    public HousekeepingServiceImpl(
            HousekeepingRoomDayStatusRepository dayStatusRepository,
            HousekeepingRoomDayStatusHistoryRepository historyRepository,
            RoomMasterProjectionRepository roomMasterProjectionRepository, HousekeepingRoomDayStatusRepository housekeepingRoomDayStatusRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.dayStatusRepository = dayStatusRepository;
        this.historyRepository = historyRepository;
        this.roomMasterProjectionRepository = roomMasterProjectionRepository;
        this.housekeepingRoomDayStatusRepository = housekeepingRoomDayStatusRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public HousekeepingDashboardResponse dashboard(UUID propertyId, LocalDate businessDate) {
        log.info(
                "{}::dashboard - Fetching dashboard for propertyId={}, businessDate={}",
                getClass().getSimpleName(),
                propertyId,
                businessDate
        );

        List<HousekeepingRoomDayStatus> rows =
                dayStatusRepository.findAllByPropertyIdAndBusinessDate(
                        propertyId,
                        businessDate
                );

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

        log.info(
                "{}::dashboard - Dashboard calculated successfully. totalRooms={}",
                getClass().getSimpleName(),
                totalRooms
        );

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

    @Override
    @Transactional(readOnly = true)
    public HousekeepingRoomsPageResponse rooms(HousekeepingRoomFilterRequest request) {
        log.info(
                "{}::rooms - Fetching rooms. propertyId={}, businessDate={}",
                getClass().getSimpleName(),
                request.propertyId(),
                request.businessDate()
        );

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 50 : request.size();

        Pageable pageable = PageRequest.of(
                page,
                size,
                buildSort(request.sortBy(), request.sortDir())
        );

        Specification<HousekeepingRoomDayStatus> specification =
                HousekeepingRoomSpecification.build(request);

        Page<HousekeepingRoomDayStatus> roomPage =
                dayStatusRepository.findAll(specification, pageable);

        List<HousekeepingRoomRowResponse> rooms = roomPage.getContent()
                .stream()
                .map(this::toRowResponse)
                .toList();

        log.info(
                "{}::rooms - Retrieved {} rooms",
                getClass().getSimpleName(),
                roomPage.getNumberOfElements()
        );

        return new HousekeepingRoomsPageResponse(
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages(),
//                request.fromDate(),
//                request.toDate(),
                buildFilters(request.propertyId(), request.businessDate()),
                rooms
        );
    }

    private HousekeepingFiltersResponse buildFilters(UUID propertyId, LocalDate businessDate) {
        log.debug(
                "{}::buildFilters - Building filter values",
                getClass().getSimpleName()
        );

        return new HousekeepingFiltersResponse(
                dayStatusRepository.findDistinctRoomTypes(propertyId, businessDate),
                dayStatusRepository.findDistinctFloors(propertyId, businessDate),
                dayStatusRepository.findDistinctAttendants(propertyId, businessDate)
        );
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String entityField = switch (sortBy == null ? "" : sortBy) {
            case "roomNumber" -> "roomNumber";
            case "arrivalDate" -> "arrivalDate";
            case "departureDate" -> "departureDate";
            case "guestName" -> "guestDisplayName";
            case "cleaningStatus" -> "cleaningStatus";
            case "frontOfficeStatus" -> "frontOfficeStatus";
            case "reservationStatus" -> "reservationStatus";
            case "attendant" -> "attendantName";
            case "priority" -> "priority";
            default -> "roomNumber";
        };

        log.debug(
                "{}::buildSort - sortBy={}, direction={}",
                getClass().getSimpleName(),
                entityField,
                direction
        );

        return Sort.by(direction, entityField);
    }

    @Override
    @Transactional(readOnly = true)
    public HousekeepingCalendarResponse calendar(
            UUID propertyId,
            LocalDate fromDate,
            LocalDate toDate,
            UUID roomTypeId
    ) {

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "fromDate must be before or equal to toDate"
            );
        }

        List<HousekeepingRoomDayStatus> records =
                housekeepingRoomDayStatusRepository.findCalendarData(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypeId
                );

        List<CalendarDateResponse> dates =
                buildCalendarDates(fromDate, toDate);

        List<CalendarRoomTypeResponse> roomTypes =
                buildRoomTypes(records, fromDate, toDate);

        return new HousekeepingCalendarResponse(
                propertyId,
                fromDate,
                toDate,
                dates,
                roomTypes
        );
    }

    private List<CalendarDateResponse> buildCalendarDates(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        return fromDate
                .datesUntil(toDate.plusDays(1))
                .map(date -> new CalendarDateResponse(
                        date,
                        date.getDayOfWeek().name(),
                        date.getDayOfMonth()
                ))
                .toList();
    }

    private List<CalendarRoomTypeResponse> buildRoomTypes(
            List<HousekeepingRoomDayStatus> records,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        Map<UUID, List<HousekeepingRoomDayStatus>> byRoomType =
                records.stream()
                        .collect(Collectors.groupingBy(
                                HousekeepingRoomDayStatus::getRoomTypeId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return byRoomType.values()
                .stream()
                .map(roomTypeRecords -> {

                    HousekeepingRoomDayStatus first =
                            roomTypeRecords.get(0);

                    Map<String, List<HousekeepingRoomDayStatus>> byRoom =
                            roomTypeRecords.stream()
                                    .collect(Collectors.groupingBy(
                                            HousekeepingRoomDayStatus::getRoomNumber,
                                            LinkedHashMap::new,
                                            Collectors.toList()
                                    ));

                    List<CalendarRoomResponse> rooms =
                            byRoom.values()
                                    .stream()
                                    .map(roomRecords -> {

                                        HousekeepingRoomDayStatus firstRoom =
                                                roomRecords.get(0);

                                        List<CalendarRoomDayResponse> days =
                                                buildRoomDays(
                                                        roomRecords,
                                                        fromDate,
                                                        toDate
                                                );

                                        return new CalendarRoomResponse(
                                                firstRoom.getRoomNumber(),
                                                firstRoom.getFloor(),
                                                days
                                        );
                                    })
                                    .toList();

                    return new CalendarRoomTypeResponse(
                            first.getRoomTypeId(),
                            first.getRoomTypeName(),
                            rooms
                    );
                })
                .toList();
    }

    private List<CalendarRoomDayResponse> buildRoomDays(
            List<HousekeepingRoomDayStatus> roomRecords,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        Map<LocalDate, HousekeepingRoomDayStatus> byDate =
                roomRecords.stream()
                        .collect(Collectors.toMap(
                                HousekeepingRoomDayStatus::getBusinessDate,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));

        return fromDate
                .datesUntil(toDate.plusDays(1))
                .map(date -> {

                    HousekeepingRoomDayStatus record =
                            byDate.get(date);

                    if (record == null) {
                        return new CalendarRoomDayResponse(
                                date,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        );
                    }

                    return new CalendarRoomDayResponse(
                            date,
                            record.getCleaningStatus() != null
                                    ? record.getCleaningStatus().name()
                                    : null,

                            record.getFrontOfficeStatus() != null
                                    ? record.getFrontOfficeStatus().name()
                                    : null,

                            record.getReservationStatus() != null
                                    ? record.getReservationStatus().name()
                                    : null,

                            record.getGuestDisplayName(),

                            record.getArrivalDate(),

                            record.getDepartureDate(),

                            record.getAttendantName(),

                            record.getPriority() != null
                                    ? record.getPriority().name()
                                    : null,

                            record.isSellable(),

                            record.getConfirmationId() != null
                                    ? record.getConfirmationId().toString()
                                    : null
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignableRoomResponse> assignableRooms(UUID propertyId, LocalDate businessDate, UUID roomTypeId, int limit) {
        log.info("HousekeepingService::assignableRooms - Fetching assignable rooms. propertyId={}, businessDate={}, roomTypeId={}, limit={}",
                propertyId, businessDate, roomTypeId, limit);

        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<HousekeepingRoomDayStatus> rows = dayStatusRepository
                .findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndConfirmationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
                        propertyId,
                        businessDate,
                        roomTypeId,
                        List.of(CleaningStatus.CLEAN, CleaningStatus.INSPECTED),
                        FrontOfficeStatus.VACANT
                );

        log.info("HousekeepingService::assignableRooms - {} eligible rooms found.", rows.size());

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

        log.info("HousekeepingService::assignableRooms - Returning {} assignable rooms.", out.size());

        return out;
    }

    @Override
    @Transactional
    public HousekeepingStatusUpdateResponse updateRoomStatus(
            String roomNumber,
            UpdateHousekeepingStatusRequest request
    ) {
        log.info("HousekeepingService::updateRoomStatus - Request received for roomNumber={}, propertyId={}, businessDate={}",
                roomNumber,
                request.propertyId(),
                request.businessDate());

        String loggedInUser = currentUserProvider.getCurrentUsername();

        log.info("HousekeepingService::updateRoomStatus - Logged in user={}", loggedInUser);

        HousekeepingRoomDayStatus row = dayStatusRepository
                .findByPropertyIdAndBusinessDateAndRoomNumber(request.propertyId(), request.businessDate(), roomNumber)
                .orElseThrow(() -> {
                    log.warn("HousekeepingService::updateRoomStatus - Room not found. propertyId={}, businessDate={}, roomNumber={}",
                            request.propertyId(),
                            request.businessDate(),
                            roomNumber);

                    return new HousekeepingNotFoundException(
                            "Housekeeping status not found for room: " + roomNumber);
                });
        LocalDateTime now = LocalDateTime.now();

        applyCleaningStatusChange(row, request, now, loggedInUser);
        applyFrontOfficeStatusChange(row, request, now, loggedInUser);
        applyReservationStatusChange(row, request, now, loggedInUser);

        if (request.confirmationId() != null || row.getConfirmationId() != null) {
            String oldValue = row.getConfirmationId();
            String newValue = request.confirmationId();
            if (!Objects.equals(oldValue, newValue)) {
                log.info("HousekeepingService::updateRoomStatus - Assigned reservation changing from {} to {}",
                        oldValue,
                        newValue);

                row.setConfirmationId(newValue);
                saveHistory(row, "assignedReservationId", oldValue, newValue, request, now, loggedInUser);
            }
        }

        if (request.attendantName() != null && !Objects.equals(row.getAttendantName(), request.attendantName())) {
            log.info("HousekeepingService::updateRoomStatus - Attendant changing from {} to {}",
                    row.getAttendantName(),
                    request.attendantName());

            saveHistory(row, "attendantName", row.getAttendantName(), request.attendantName(), request, now, loggedInUser);
            row.setAttendantName(request.attendantName());
        }

        if (request.priority() != null && request.priority() != row.getPriority()) {
            log.info("HousekeepingService::updateRoomStatus - Priority changing from {} to {}",
                    row.getPriority(),
                    request.priority());
            saveHistory(row, "priority", row.getPriority().name(), request.priority().name(), request, now, loggedInUser);
            row.setPriority(request.priority());
        }

        if (request.guestDisplayName() != null) {
            log.info("HousekeepingService::updateRoomStatus - Guest display name changing from {} to {}",
                    row.getGuestDisplayName(),
                    request.guestDisplayName());
            row.setGuestDisplayName(request.guestDisplayName());
        }
        if (request.arrivalDate() != null) {
            row.setArrivalDate(request.arrivalDate());
        }
        if (request.departureDate() != null) {
            row.setDepartureDate(request.departureDate());
        }

        row.setSellable(request.sellable() != null ? request.sellable() : computeSellable(row));

        row.setUpdatedBy(loggedInUser);
        row.setUpdatedAt(now);

        HousekeepingRoomDayStatus saved = dayStatusRepository.save(row);
        log.info("HousekeepingService::updateRoomStatus - Successfully updated room {}. CleaningStatus={}, FrontOfficeStatus={}, ReservationStatus={}, Sellable={}",
                saved.getRoomNumber(),
                saved.getCleaningStatus(),
                saved.getFrontOfficeStatus(),
                saved.getReservationStatus(),
                saved.isSellable());

        return new HousekeepingStatusUpdateResponse(
                saved.getPropertyId(),
                saved.getBusinessDate(),
                saved.getRoomNumber(),
                saved.getCleaningStatus().name(),
                saved.getFrontOfficeStatus().name(),
                saved.getReservationStatus().name(),
                saved.getAttendantName(),
                saved.getPriority(),
                saved.getConfirmationId(),
                saved.isSellable(),
                saved.getUpdatedAt(),
                saved.getLastCleanedAt()
        );
    }

    private void applyCleaningStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now, String loggedInUser) {
        if (request.cleaningStatus() == null || request.cleaningStatus() == row.getCleaningStatus()) {
            log.debug("HousekeepingService::applyCleaningStatusChange - No cleaning status change for room {}",
                    row.getRoomNumber());
            return;
        }

        log.info(
                "HousekeepingService::applyCleaningStatusChange - Room {} cleaning status changing from {} to {}",
                row.getRoomNumber(),
                row.getCleaningStatus(),
                request.cleaningStatus()
        );

        saveHistory(row, "cleaningStatus", row.getCleaningStatus().name(), request.cleaningStatus().name(), request, now, loggedInUser);
        row.setCleaningStatus(request.cleaningStatus());
        if (request.cleaningStatus() == CleaningStatus.CLEAN ) {
            row.setLastCleanedAt(now);

            log.info(
                    "HousekeepingService::applyCleaningStatusChange - lastCleanedAt updated for room {}",
                    row.getRoomNumber()
            );
        }
    }

    private void applyFrontOfficeStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now, String loggedInUser) {
        if (request.frontOfficeStatus() == null || request.frontOfficeStatus() == row.getFrontOfficeStatus()) {
            return;
        }
        log.info(
                "HousekeepingService::applyFrontOfficeStatusChange - Room {} front office status changing from {} to {}",
                row.getRoomNumber(),
                row.getFrontOfficeStatus(),
                request.frontOfficeStatus()
        );
        saveHistory(row, "frontOfficeStatus", row.getFrontOfficeStatus().name(), request.frontOfficeStatus().name(), request, now, loggedInUser);
        row.setFrontOfficeStatus(request.frontOfficeStatus());
    }

    private void applyReservationStatusChange(HousekeepingRoomDayStatus row, UpdateHousekeepingStatusRequest request, LocalDateTime now, String loggedInUser) {
        if (request.reservationStatus() == null || request.reservationStatus() == row.getReservationStatus()) {
            return;
        }
        log.info(
                "HousekeepingService::applyReservationStatusChange - Room {} reservation status changing from {} to {}",
                row.getRoomNumber(),
                row.getReservationStatus(),
                request.reservationStatus()
        );
        saveHistory(row, "reservationStatus", row.getReservationStatus().name(), request.reservationStatus().name(), request, now, loggedInUser);
        row.setReservationStatus(request.reservationStatus());
    }

    private void saveHistory(
            HousekeepingRoomDayStatus row,
            String field,
            String oldValue,
            String newValue,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {
        log.debug(
                "HousekeepingService::saveHistory - Recording history for room {}, field={}, old={}, new={}",
                row.getRoomNumber(),
                field,
                oldValue,
                newValue
        );

        HousekeepingRoomDayStatusHistory history = HousekeepingRoomDayStatusHistory.builder()
                .propertyId(row.getPropertyId())
                .businessDate(row.getBusinessDate())
                .roomNumber(row.getRoomNumber())
                .changedField(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(now)
                .changedBy(loggedInUser)
                .sourceModule(request.sourceModule())
                .build();
        historyRepository.save(history);
    }

    private long count(List<HousekeepingRoomDayStatus> rows, java.util.function.Predicate<HousekeepingRoomDayStatus> predicate) {
        return rows.stream().filter(predicate).count();
    }

    private HousekeepingRoomRowResponse toRowResponse(HousekeepingRoomDayStatus room) {
        return new HousekeepingRoomRowResponse(
                room.getRoomNumber(),
                room.getRoomTypeId(),
                room.getRoomTypeName(),
                room.getFloor(),
                room.getCleaningStatus().name(),
                room.getFrontOfficeStatus().name(),
                room.getReservationStatus().name(),
                room.getGuestDisplayName(),
                room.getArrivalDate(),
                room.getDepartureDate(),
                room.getAttendantName(),
                room.getLastCleanedAt(),
                room.getPriority(),
                room.isSellable(),
                room.getConfirmationId(),
                room.getFeaturesCsv()
        );
    }

    private boolean computeSellable(HousekeepingRoomDayStatus status) {
        boolean cleanEnough = status.getCleaningStatus() == CleaningStatus.CLEAN
                || status.getCleaningStatus() == CleaningStatus.INSPECTED;
        boolean vacant = status.getFrontOfficeStatus() == FrontOfficeStatus.VACANT;
        boolean noAssignment = status.getConfirmationId() == null;
        boolean notOut = status.getCleaningStatus() != CleaningStatus.OUT_OF_ORDER
                && status.getCleaningStatus() != CleaningStatus.OUT_OF_SERVICE;
        return cleanEnough && vacant && noAssignment && notOut;
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}

