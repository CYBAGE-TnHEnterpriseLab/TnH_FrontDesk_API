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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HousekeepingServiceImpl implements HousekeepingService {

    private static final Logger log =
            LoggerFactory.getLogger(HousekeepingServiceImpl.class);

    private final HousekeepingRoomDayStatusRepository dayStatusRepository;
    private final HousekeepingRoomDayStatusHistoryRepository historyRepository;
    private final RoomMasterProjectionRepository roomMasterProjectionRepository;
    private final CurrentUserProvider currentUserProvider;

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public HousekeepingDashboardResponse dashboard(
            UUID propertyId,
            LocalDate businessDate
    ) {

        log.info(
                "Fetching housekeeping dashboard. propertyId={}, businessDate={}",
                propertyId,
                businessDate
        );

        List<HousekeepingRoomDayStatus> rows =
                dayStatusRepository.findAllByPropertyIdAndBusinessDate(
                        propertyId,
                        businessDate
                );

        long totalRooms = rows.size();

        long vacantClean = count(rows, row ->
                row.getFrontOfficeStatus() == FrontOfficeStatus.VACANT
                        && row.getCleaningStatus() == CleaningStatus.CLEAN
        );

        long vacantDirty = count(rows, row ->
                row.getFrontOfficeStatus() == FrontOfficeStatus.VACANT
                        && row.getCleaningStatus() == CleaningStatus.DIRTY
        );

        long occupiedClean = count(rows, row ->
                row.getFrontOfficeStatus() == FrontOfficeStatus.OCCUPIED
                        && row.getCleaningStatus() == CleaningStatus.CLEAN
        );

        long occupiedDirty = count(rows, row ->
                row.getFrontOfficeStatus() == FrontOfficeStatus.OCCUPIED
                        && row.getCleaningStatus() == CleaningStatus.DIRTY
        );

        long outOfOrder = count(rows,
                row -> row.getCleaningStatus() == CleaningStatus.OUT_OF_ORDER);

        long outOfService = count(rows,
                row -> row.getCleaningStatus() == CleaningStatus.OUT_OF_SERVICE);

        long inspected = count(rows,
                row -> row.getCleaningStatus() == CleaningStatus.INSPECTED);

        long pickup = count(rows,
                row -> row.getCleaningStatus() == CleaningStatus.PICKUP);

        long arrivals = count(
                rows,
                row -> row.getReservationStatus() == ReservationStatus.ARRIVAL
        );

        long departures = count(
                rows,
                row -> row.getReservationStatus() == ReservationStatus.DEPARTURE
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

    // =========================================================
    // ROOMS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public HousekeepingRoomsPageResponse rooms(
            HousekeepingRoomFilterRequest request
    ) {

        log.info(
                "{}::rooms - Fetching rooms. propertyId={}, businessDate={}, cleaningStatus={}",
                getClass().getSimpleName(),
                request.propertyId(),
                request.businessDate(),
                request.cleaningStatus()
        );

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 50 : request.size();

        Sort sort = buildSort(
                request.sortBy(),
                request.sortDir()
        );

        /*
         * Cleaning status can now safely be filtered by the
         * Specification because future dates are physically
         * updated in housekeeping_room_day_status.
         */
        Specification<HousekeepingRoomDayStatus> specification =
                HousekeepingRoomSpecification.build(request);

        List<HousekeepingRoomDayStatus> allRows =
                dayStatusRepository.findAll(
                        specification,
                        sort
                );

        long totalElements = allRows.size();

        int fromIndex = Math.min(
                page * size,
                allRows.size()
        );

        int toIndex = Math.min(
                fromIndex + size,
                allRows.size()
        );

        List<HousekeepingRoomRowResponse> rooms =
                allRows.subList(fromIndex, toIndex)
                        .stream()
                        .map(this::toRowResponse)
                        .toList();

        int totalPages =
                totalElements == 0
                        ? 0
                        : (int) Math.ceil(
                        (double) totalElements / size
                );

        return new HousekeepingRoomsPageResponse(
                page,
                size,
                totalElements,
                totalPages,
                buildFilters(
                        request.propertyId(),
                        request.businessDate()
                ),
                rooms
        );
    }

    private HousekeepingFiltersResponse buildFilters(
            UUID propertyId,
            LocalDate businessDate
    ) {

        return new HousekeepingFiltersResponse(
                dayStatusRepository.findDistinctRoomTypes(
                        propertyId,
                        businessDate
                ),
                dayStatusRepository.findDistinctFloors(
                        propertyId,
                        businessDate
                ),
                dayStatusRepository.findDistinctAttendants(
                        propertyId,
                        businessDate
                )
        );
    }

    private Sort buildSort(
            String sortBy,
            String sortDirection
    ) {

        Sort.Direction direction =
                "desc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        String entityField = switch (
                sortBy == null ? "" : sortBy
                ) {

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

        return Sort.by(
                direction,
                entityField
        );
    }

    // =========================================================
    // CALENDAR
    // =========================================================

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
                dayStatusRepository.findCalendarData(
                        propertyId,
                        fromDate,
                        toDate,
                        roomTypeId
                );

        List<CalendarDateResponse> dates =
                buildCalendarDates(
                        fromDate,
                        toDate
                );

        List<CalendarRoomTypeResponse> roomTypes =
                buildRoomTypes(
                        records,
                        fromDate,
                        toDate
                );

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
                .map(date ->
                        new CalendarDateResponse(
                                date,
                                date.getDayOfWeek().name(),
                                date.getDayOfMonth()
                        )
                )
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
                            roomTypeRecords.getFirst();

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
                                                roomRecords.getFirst();

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
                                false,
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
                            record.getConfirmationId()
                    );
                })
                .toList();
    }

    // =========================================================
    // ASSIGNABLE ROOMS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignableRoomResponse> assignableRooms(
            UUID propertyId,
            LocalDate businessDate,
            UUID roomTypeId,
            int limit
    ) {

        int safeLimit = Math.min(
                Math.max(limit, 1),
                200
        );

        List<HousekeepingRoomDayStatus> rows =
                dayStatusRepository
                        .findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndConfirmationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
                                propertyId,
                                businessDate,
                                roomTypeId,
                                List.of(
                                        CleaningStatus.CLEAN,
                                        CleaningStatus.INSPECTED
                                ),
                                FrontOfficeStatus.VACANT
                        );

        Map<String, RoomMasterProjection> roomMasterByNumber =
                roomMasterProjectionRepository
                        .findAllByPropertyId(propertyId)
                        .stream()
                        .collect(Collectors.toMap(
                                RoomMasterProjection::getRoomNumber,
                                Function.identity(),
                                (left, right) -> left,
                                HashMap::new
                        ));

        List<AssignableRoomResponse> result =
                new ArrayList<>();

        for (HousekeepingRoomDayStatus row : rows) {

            RoomMasterProjection projection =
                    roomMasterByNumber.get(
                            row.getRoomNumber()
                    );

            result.add(
                    new AssignableRoomResponse(
                            row.getRoomNumber(),
                            row.getRoomTypeId(),
                            projection == null
                                    ? null
                                    : projection.getRoomTypeName(),
                            projection == null
                                    ? null
                                    : projection.getFloor(),
                            projection == null
                                    ? null
                                    : projection.getRoomClass(),
                            projection == null
                                    ? null
                                    : projection.getZone(),
                            row.getCleaningStatus().name()
                    )
            );

            if (result.size() >= safeLimit) {
                break;
            }
        }

        return result;
    }

    // =========================================================
    // UPDATE ROOM STATUS
    // =========================================================

    @Override
    @Transactional
    public HousekeepingStatusUpdateResponse updateRoomStatus(
            String roomNumber,
            UpdateHousekeepingStatusRequest request
    ) {

        log.info(
                "Updating room status. roomNumber={}, propertyId={}, businessDate={}",
                roomNumber,
                request.propertyId(),
                request.businessDate()
        );

        String loggedInUser =
                currentUserProvider.getCurrentUsername();

        HousekeepingRoomDayStatus row =
                dayStatusRepository
                        .findByPropertyIdAndBusinessDateAndRoomNumber(
                                request.propertyId(),
                                request.businessDate(),
                                roomNumber
                        )
                        .orElseThrow(() ->
                                new HousekeepingNotFoundException(
                                        "Housekeeping status not found for room: "
                                                + roomNumber
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        applyCleaningStatusChange(
                row,
                request,
                now,
                loggedInUser
        );

        applyFrontOfficeStatusChange(
                row,
                request,
                now,
                loggedInUser
        );

        applyReservationStatusChange(
                row,
                request,
                now,
                loggedInUser
        );

        updateConfirmationId(
                row,
                request,
                now,
                loggedInUser
        );

        updateAttendant(
                row,
                request,
                now,
                loggedInUser
        );

        updatePriority(
                row,
                request,
                now,
                loggedInUser
        );

        if (request.guestDisplayName() != null) {
            row.setGuestDisplayName(
                    request.guestDisplayName()
            );
        }

        if (request.arrivalDate() != null) {
            row.setArrivalDate(
                    request.arrivalDate()
            );
        }

        if (request.departureDate() != null) {
            row.setDepartureDate(
                    request.departureDate()
            );
        }

        row.setSellable(
                request.sellable() != null
                        ? request.sellable()
                        : computeSellable(row)
        );

        row.setUpdatedBy(loggedInUser);
        row.setUpdatedAt(now);

        HousekeepingRoomDayStatus saved =
                dayStatusRepository.saveAndFlush(row);

        return new HousekeepingStatusUpdateResponse(
                saved.getPropertyId(),
                saved.getBusinessDate(),
                saved.getRoomNumber(),
                saved.getCleaningStatus() != null
                        ? saved.getCleaningStatus().name()
                        : null,
                saved.getFrontOfficeStatus() != null
                        ? saved.getFrontOfficeStatus().name()
                        : null,
                saved.getReservationStatus() != null
                        ? saved.getReservationStatus().name()
                        : null,
                saved.getAttendantName(),
                saved.getPriority(),
                saved.getConfirmationId(),
                saved.isSellable(),
                saved.getUpdatedAt(),
                saved.getLastCleanedAt()
        );
    }

    // =========================================================
    // CLEANING STATUS
    // =========================================================

    private void applyCleaningStatusChange(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        CleaningStatus requestedStatus =
                request.cleaningStatus();

        if (requestedStatus == null
                || requestedStatus == row.getCleaningStatus()) {
            return;
        }

        CleaningStatus oldStatus =
                row.getCleaningStatus();

        saveHistory(
                row,
                "cleaningStatus",
                toStringValue(oldStatus),
                toStringValue(requestedStatus),
                request,
                now,
                loggedInUser
        );

        /*
         * Update the current business date.
         */
        row.setCleaningStatus(requestedStatus);

        if (requestedStatus == CleaningStatus.CLEAN) {

            row.setLastCleanedAt(now);

            log.info(
                    "Room {} marked CLEAN on {}. Propagating CLEAN to future dates.",
                    row.getRoomNumber(),
                    row.getBusinessDate()
            );

            /*
             * IMPORTANT:
             *
             * Current row is handled by JPA.
             * Future rows are physically updated using one bulk UPDATE.
             */
            dayStatusRepository.updateCleaningStatusFromDate(
                    row.getPropertyId(),
                    row.getRoomNumber(),
                    row.getBusinessDate().plusDays(1),
                    CleaningStatus.CLEAN,
                    now,
                    now,
                    loggedInUser
            );

        } else if (requestedStatus == CleaningStatus.DIRTY) {

            row.setLastCleanedAt(null);

            log.info(
                    "Room {} marked DIRTY on {}. Propagating DIRTY to future dates.",
                    row.getRoomNumber(),
                    row.getBusinessDate()
            );

            dayStatusRepository.updateCleaningStatusFromDateAfterCheckout(
                    row.getPropertyId(),
                    row.getRoomNumber(),
                    row.getBusinessDate().plusDays(1),
                    CleaningStatus.DIRTY,
                    now,
                    loggedInUser
            );
        }
    }

    // =========================================================
    // FRONT OFFICE STATUS
    // =========================================================

    private void applyFrontOfficeStatusChange(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        if (request.frontOfficeStatus() == null
                || request.frontOfficeStatus()
                == row.getFrontOfficeStatus()) {
            return;
        }

        saveHistory(
                row,
                "frontOfficeStatus",
                toStringValue(row.getFrontOfficeStatus()),
                toStringValue(request.frontOfficeStatus()),
                request,
                now,
                loggedInUser
        );

        row.setFrontOfficeStatus(
                request.frontOfficeStatus()
        );
    }

    // =========================================================
    // RESERVATION STATUS
    // =========================================================

    private void applyReservationStatusChange(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        if (request.reservationStatus() == null
                || request.reservationStatus()
                == row.getReservationStatus()) {
            return;
        }

        saveHistory(
                row,
                "reservationStatus",
                toStringValue(row.getReservationStatus()),
                toStringValue(request.reservationStatus()),
                request,
                now,
                loggedInUser
        );

        row.setReservationStatus(
                request.reservationStatus()
        );

        /*
         * CHECKED_OUT is the housekeeping reset event.
         *
         * Current date + all future dates become DIRTY.
         */
        if (request.reservationStatus()
                == ReservationStatus.CHECKED_OUT) {

            CleaningStatus oldStatus =
                    row.getCleaningStatus();

            if (oldStatus != CleaningStatus.DIRTY) {

                saveHistory(
                        row,
                        "cleaningStatus",
                        toStringValue(oldStatus),
                        CleaningStatus.DIRTY.name(),
                        request,
                        now,
                        loggedInUser
                );
            }

            row.setCleaningStatus(
                    CleaningStatus.DIRTY
            );

            row.setLastCleanedAt(null);

            /*
             * Physically reset every future day.
             */
            dayStatusRepository.updateCleaningStatusFromDateAfterCheckout(
                    row.getPropertyId(),
                    row.getRoomNumber(),
                    row.getBusinessDate().plusDays(1),
                    CleaningStatus.DIRTY,
                    now,
                    loggedInUser
            );

            log.info(
                    "Room {} checked out on {}. Current and future cleaning status set to DIRTY.",
                    row.getRoomNumber(),
                    row.getBusinessDate()
            );
        }
    }

    // =========================================================
    // CONFIRMATION ID
    // =========================================================

    private void updateConfirmationId(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        if (request.confirmationId() == null
                && row.getConfirmationId() == null) {
            return;
        }

        String oldValue =
                row.getConfirmationId();

        String newValue =
                request.confirmationId();

        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        saveHistory(
                row,
                "confirmationId",
                oldValue,
                newValue,
                request,
                now,
                loggedInUser
        );

        row.setConfirmationId(newValue);
    }

    // =========================================================
    // ATTENDANT
    // =========================================================

    private void updateAttendant(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        if (request.attendantName() == null
                || Objects.equals(
                row.getAttendantName(),
                request.attendantName())) {
            return;
        }

        saveHistory(
                row,
                "attendantName",
                row.getAttendantName(),
                request.attendantName(),
                request,
                now,
                loggedInUser
        );

        row.setAttendantName(
                request.attendantName()
        );
    }

    // =========================================================
    // PRIORITY
    // =========================================================

    private void updatePriority(
            HousekeepingRoomDayStatus row,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        if (request.priority() == null
                || request.priority() == row.getPriority()) {
            return;
        }

        saveHistory(
                row,
                "priority",
                toStringValue(row.getPriority()),
                toStringValue(request.priority()),
                request,
                now,
                loggedInUser
        );

        row.setPriority(
                request.priority()
        );
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private HousekeepingRoomRowResponse toRowResponse(
            HousekeepingRoomDayStatus room
    ) {

        /*
         * No history lookup is required anymore.
         *
         * cleaningStatus in housekeeping_room_day_status is
         * already the effective physical status.
         */
        return new HousekeepingRoomRowResponse(
                room.getRoomNumber(),
                room.getRoomTypeId(),
                room.getRoomTypeName(),
                room.getFloor(),

                room.getCleaningStatus() != null
                        ? room.getCleaningStatus().name()
                        : null,

                room.getFrontOfficeStatus() != null
                        ? room.getFrontOfficeStatus().name()
                        : null,

                room.getReservationStatus() != null
                        ? room.getReservationStatus().name()
                        : null,

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

    // =========================================================
    // SELLABLE
    // =========================================================

    private boolean computeSellable(
            HousekeepingRoomDayStatus status
    ) {

        CleaningStatus cleaningStatus =
                status.getCleaningStatus();

        boolean cleanEnough =
                cleaningStatus == CleaningStatus.CLEAN
                        || cleaningStatus == CleaningStatus.INSPECTED;

        boolean vacant =
                status.getFrontOfficeStatus()
                        == FrontOfficeStatus.VACANT;

        boolean noAssignment =
                status.getConfirmationId() == null;

        boolean notOut =
                cleaningStatus != CleaningStatus.OUT_OF_ORDER
                        && cleaningStatus != CleaningStatus.OUT_OF_SERVICE;

        return cleanEnough
                && vacant
                && noAssignment
                && notOut;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void saveHistory(
            HousekeepingRoomDayStatus row,
            String field,
            String oldValue,
            String newValue,
            UpdateHousekeepingStatusRequest request,
            LocalDateTime now,
            String loggedInUser
    ) {

        HousekeepingRoomDayStatusHistory history =
                HousekeepingRoomDayStatusHistory.builder()
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

    // =========================================================
    // UTILITY
    // =========================================================

    private long count(
            List<HousekeepingRoomDayStatus> rows,
            java.util.function.Predicate<HousekeepingRoomDayStatus> predicate
    ) {

        return rows.stream()
                .filter(predicate)
                .count();
    }

    private String toStringValue(Object value) {
        return value == null
                ? null
                : value.toString();
    }
}