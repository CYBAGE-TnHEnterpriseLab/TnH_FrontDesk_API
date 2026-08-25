package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.service.ReservationRoomCalendarService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationRoomCalendarServiceImpl implements ReservationRoomCalendarService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_BOOKED = "BOOKED";
    private static final String STATUS_OCCUPIED = "OCCUPIED";
    private static final String STATUS_DIRTY = "DIRTY";
    private static final String STATUS_CLEANED = "CLEANED";

    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final PropertyInventoryPort propertyInventoryPort;
    private final HousekeepingRoomCalendarClient housekeepingRoomCalendarClient;
    private final ReservationBookingRepository reservationBookingRepository;
    private final HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @Override
    @Transactional(readOnly = true)
    public ReservationRoomCalendarResponseDto getRoomCalendar(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<String> roomTypes
    ) {
        if (!StringUtils.hasText(propertyId)) {
            throw new BadRequestException("propertyId is required");
        }
        if (arrivalDate == null || departureDate == null) {
            throw new BadRequestException("arrivalDate and departureDate are required");
        }
        if (departureDate.isBefore(arrivalDate)) {
            throw new BadRequestException("departureDate must be on or after arrivalDate");
        }
        List<LocalDate> dates = buildDateRangeInclusive(arrivalDate, departureDate);
        LocalDate overlapUpperExclusive = departureDate.plusDays(1);

        List<ReservationBookingRecord> overlappingBookings = reservationBookingRepository
                .findByPropertyIdAndAssignedRoomNoIsNotNullAndArrivalDateLessThanAndDepartureDateGreaterThan(
                        propertyId,
                        overlapUpperExclusive,
                        arrivalDate
                );

        List<String> effectiveRoomTypes = sanitizeRoomTypes(roomTypes);
        Set<String> requestedRoomTypes = normalizeRoomTypes(effectiveRoomTypes);
        String upstreamRoomTypeFilter = requestedRoomTypes.size() == 1
            ? requestedRoomTypes.iterator().next()
            : null;

        List<PropertyRoomInventoryDto> liveInventory = housekeepingRoomCalendarClient.fetchRooms(
                propertyId, arrivalDate, departureDate);
        if (liveInventory == null) {
            liveInventory = List.of();
        }

        List<HousekeepingRoomStatusRecord> housekeepingStatuses = housekeepingRoomStatusRepository
                .findByPropertyIdAndBusinessDateBetweenAndRoomNoIsNotNull(propertyId, arrivalDate, departureDate);

        Map<String, RoomMeta> roomMetaByNo = collectRoomMeta(
                liveInventory,
                overlappingBookings,
                housekeepingStatuses,
            requestedRoomTypes
        );

        if (roomMetaByNo.isEmpty()) {
            return ReservationRoomCalendarResponseDto.builder()
                    .propertyId(propertyId)
                    .arrivalDate(arrivalDate)
                    .departureDate(departureDate)
                    .roomTypes(effectiveRoomTypes)
                    .dates(dates)
                    .rooms(List.of())
                    .summary(List.of())
                    .build();
        }

        Map<String, BookingRef> bookingRefByConfirmation = buildBookingReferenceMap(overlappingBookings);
        Map<String, Map<LocalDate, MutableCell>> grid = initializeGrid(roomMetaByNo.keySet(), dates);

        applyBookingStatuses(grid, overlappingBookings, arrivalDate, departureDate, requestedRoomTypes);
        applyHousekeepingStatuses(grid, housekeepingStatuses, bookingRefByConfirmation, requestedRoomTypes, roomMetaByNo);

        List<ReservationRoomCalendarResponseDto.RoomCalendarRowDto> roomRows = buildRoomRows(roomMetaByNo, grid, dates);
        List<ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto> summary = buildDaySummary(dates, roomRows);

        return ReservationRoomCalendarResponseDto.builder()
                .propertyId(propertyId)
                .arrivalDate(arrivalDate)
                .departureDate(departureDate)
            .roomTypes(effectiveRoomTypes)
                .dates(dates)
                .rooms(roomRows)
                .summary(summary)
                .build();
    }

    private Map<String, RoomMeta> collectRoomMeta(
            List<PropertyRoomInventoryDto> liveInventory,
            List<ReservationBookingRecord> overlappingBookings,
            List<HousekeepingRoomStatusRecord> housekeepingStatuses,
            Set<String> requestedRoomTypes
    ) {
        Map<String, RoomMeta> roomMetaByNo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (PropertyRoomInventoryDto inventoryItem : liveInventory) {
            if (inventoryItem == null || !StringUtils.hasText(inventoryItem.getRoomNumber())) {
                continue;
            }

            String inventoryRoomType = inventoryItem.getRoomType();
            if (!matchesRequestedRoomType(inventoryRoomType, requestedRoomTypes)) {
                continue;
            }

            String roomNo = inventoryItem.getRoomNumber().trim();
            roomMetaByNo.compute(roomNo, (ignored, existing) -> {
                if (existing == null) {
                    return new RoomMeta(roomNo, inventoryRoomType, null);
                }
                if (!StringUtils.hasText(existing.roomType) && StringUtils.hasText(inventoryRoomType)) {
                    existing.roomType = inventoryRoomType;
                }
                return existing;
            });
        }

        for (ReservationBookingRecord booking : overlappingBookings) {
            if (!StringUtils.hasText(booking.getAssignedRoomNo())) {
                continue;
            }
            if (!matchesRequestedRoomType(booking.getRoomType(), requestedRoomTypes)) {
                continue;
            }

            String roomNo = booking.getAssignedRoomNo().trim();
            roomMetaByNo.compute(roomNo, (ignored, existing) -> {
                if (existing == null) {
                    return new RoomMeta(roomNo, booking.getRoomType(), booking.getFloor());
                }
                if (!StringUtils.hasText(existing.roomType) && StringUtils.hasText(booking.getRoomType())) {
                    existing.roomType = booking.getRoomType();
                }
                if (existing.floor == null && booking.getFloor() != null) {
                    existing.floor = booking.getFloor();
                }
                return existing;
            });
        }

        for (HousekeepingRoomStatusRecord housekeepingStatus : housekeepingStatuses) {
            if (!StringUtils.hasText(housekeepingStatus.getRoomNo())) {
                continue;
            }

            String roomNo = housekeepingStatus.getRoomNo().trim();
            RoomMeta existing = roomMetaByNo.get(roomNo);
            if (existing == null && requestedRoomTypes.isEmpty()) {
                roomMetaByNo.put(roomNo, new RoomMeta(roomNo, null, null));
            }
        }

        return roomMetaByNo;
    }

    private Map<String, BookingRef> buildBookingReferenceMap(List<ReservationBookingRecord> overlappingBookings) {
        Map<String, BookingRef> bookingRefByConfirmation = new LinkedHashMap<>();
        for (ReservationBookingRecord booking : overlappingBookings) {
            if (!StringUtils.hasText(booking.getConfirmationNumber())) {
                continue;
            }
            bookingRefByConfirmation.putIfAbsent(
                    booking.getConfirmationNumber(),
                    new BookingRef(booking.getId(), booking.getReservationStatus())
            );
        }
        return bookingRefByConfirmation;
    }

    private Map<String, Map<LocalDate, MutableCell>> initializeGrid(Iterable<String> roomNumbers, List<LocalDate> dates) {
        Map<String, Map<LocalDate, MutableCell>> grid = new LinkedHashMap<>();
        for (String roomNo : roomNumbers) {
            Map<LocalDate, MutableCell> cells = new LinkedHashMap<>();
            for (LocalDate date : dates) {
                cells.put(date, MutableCell.available());
            }
            grid.put(roomNo, cells);
        }
        return grid;
    }

    private void applyBookingStatuses(
            Map<String, Map<LocalDate, MutableCell>> grid,
            List<ReservationBookingRecord> overlappingBookings,
            LocalDate calendarStart,
            LocalDate calendarEnd,
            Set<String> requestedRoomTypes
    ) {
        LocalDate calendarEndExclusive = calendarEnd.plusDays(1);

        for (ReservationBookingRecord booking : overlappingBookings) {
            if (!StringUtils.hasText(booking.getAssignedRoomNo())) {
                continue;
            }
            if (!matchesRequestedRoomType(booking.getRoomType(), requestedRoomTypes)) {
                continue;
            }
            if (!isActiveReservation(booking.getReservationStatus())) {
                continue;
            }

            String roomNo = booking.getAssignedRoomNo().trim();
            Map<LocalDate, MutableCell> roomCells = grid.get(roomNo);
            if (roomCells == null) {
                continue;
            }

            LocalDate stayStart = booking.getArrivalDate();
            LocalDate stayEndExclusive = booking.getDepartureDate();
            if (stayStart == null || stayEndExclusive == null) {
                continue;
            }

            LocalDate effectiveStart = max(calendarStart, stayStart);
            LocalDate effectiveEndExclusive = min(calendarEndExclusive, stayEndExclusive);
            if (!effectiveStart.isBefore(effectiveEndExclusive)) {
                continue;
            }

            String cellStatus = resolveBookingCellStatus(booking.getReservationStatus());
            for (LocalDate date = effectiveStart; date.isBefore(effectiveEndExclusive); date = date.plusDays(1)) {
                MutableCell cell = roomCells.get(date);
                if (cell == null) {
                    continue;
                }

                applyCellStatus(
                        cell,
                        cellStatus,
                        booking.getConfirmationNumber(),
                        booking.getId(),
                        booking.getReservationStatus()
                );
            }
        }
    }

    private void applyHousekeepingStatuses(
            Map<String, Map<LocalDate, MutableCell>> grid,
            List<HousekeepingRoomStatusRecord> housekeepingStatuses,
            Map<String, BookingRef> bookingRefByConfirmation,
            Set<String> requestedRoomTypes,
            Map<String, RoomMeta> roomMetaByNo
    ) {
        for (HousekeepingRoomStatusRecord housekeepingStatus : housekeepingStatuses) {
            if (!StringUtils.hasText(housekeepingStatus.getRoomNo()) || housekeepingStatus.getBusinessDate() == null) {
                continue;
            }

            String roomNo = housekeepingStatus.getRoomNo().trim();
            RoomMeta roomMeta = roomMetaByNo.get(roomNo);
                if (!requestedRoomTypes.isEmpty()
                    && roomMeta != null
                    && StringUtils.hasText(roomMeta.roomType)
                    && !matchesRequestedRoomType(roomMeta.roomType, requestedRoomTypes)) {
                continue;
            }

            Map<LocalDate, MutableCell> roomCells = grid.get(roomNo);
            if (roomCells == null) {
                continue;
            }

            MutableCell cell = roomCells.get(housekeepingStatus.getBusinessDate());
            if (cell == null) {
                continue;
            }

            String normalizedStatus = normalizeHousekeepingStatus(housekeepingStatus.getRoomStatus());
            String confirmationNumber = housekeepingStatus.getConfirmationNumber();
            BookingRef bookingRef = StringUtils.hasText(confirmationNumber)
                    ? bookingRefByConfirmation.get(confirmationNumber)
                    : null;

            applyCellStatus(
                    cell,
                    normalizedStatus,
                    confirmationNumber,
                    bookingRef == null ? null : bookingRef.bookingId,
                    bookingRef == null ? cell.reservationStatus : bookingRef.reservationStatus
            );
        }
    }

    private List<ReservationRoomCalendarResponseDto.RoomCalendarRowDto> buildRoomRows(
            Map<String, RoomMeta> roomMetaByNo,
            Map<String, Map<LocalDate, MutableCell>> grid,
            List<LocalDate> dates
    ) {
        return roomMetaByNo.values().stream()
                .sorted(Comparator
                        .comparing((RoomMeta item) -> item.floor, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(item -> item.roomNo, String.CASE_INSENSITIVE_ORDER))
                .map(roomMeta -> {
                    Map<LocalDate, MutableCell> roomCells = grid.get(roomMeta.roomNo);
                    List<ReservationRoomCalendarResponseDto.RoomCalendarCellDto> calendarCells = dates.stream()
                            .map(date -> {
                                MutableCell cell = roomCells.get(date);
                                return ReservationRoomCalendarResponseDto.RoomCalendarCellDto.builder()
                                        .date(date)
                                        .status(cell.status)
                                        .confirmationNumber(cell.confirmationNumber)
                                        .bookingId(cell.bookingId)
                                        .reservationStatus(cell.reservationStatus)
                                        .build();
                            })
                            .toList();

                    return ReservationRoomCalendarResponseDto.RoomCalendarRowDto.builder()
                            .roomNo(roomMeta.roomNo)
                            .roomType(roomMeta.roomType)
                            .floor(roomMeta.floor)
                            .calendar(calendarCells)
                            .build();
                })
                .toList();
    }

    private List<ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto> buildDaySummary(
            List<LocalDate> dates,
            List<ReservationRoomCalendarResponseDto.RoomCalendarRowDto> roomRows
    ) {
        List<ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto> summary = new ArrayList<>();

        for (LocalDate date : dates) {
            int totalRooms = roomRows.size();
            int availableRooms = 0;
            int bookedRooms = 0;
            int occupiedRooms = 0;
            int dirtyRooms = 0;
            int cleanedRooms = 0;

            for (ReservationRoomCalendarResponseDto.RoomCalendarRowDto room : roomRows) {
                ReservationRoomCalendarResponseDto.RoomCalendarCellDto cell = room.getCalendar().stream()
                        .filter(item -> Objects.equals(item.getDate(), date))
                        .findFirst()
                        .orElse(null);
                if (cell == null || !StringUtils.hasText(cell.getStatus())) {
                    continue;
                }

                String normalizedStatus = normalize(cell.getStatus());
                switch (normalizedStatus) {
                    case STATUS_AVAILABLE -> availableRooms++;
                    case STATUS_BOOKED -> bookedRooms++;
                    case STATUS_OCCUPIED -> occupiedRooms++;
                    case STATUS_DIRTY -> dirtyRooms++;
                    case STATUS_CLEANED -> cleanedRooms++;
                    default -> {
                        // Unknown statuses are left out from summary buckets.
                    }
                }
            }

            summary.add(ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto.builder()
                    .date(date)
                    .totalRooms(totalRooms)
                    .assignableRooms(availableRooms + cleanedRooms)
                    .availableRooms(availableRooms)
                    .bookedRooms(bookedRooms)
                    .occupiedRooms(occupiedRooms)
                    .dirtyRooms(dirtyRooms)
                    .cleanedRooms(cleanedRooms)
                    .build());
        }

        return summary;
    }

    private void applyCellStatus(
            MutableCell cell,
            String status,
            String confirmationNumber,
            Long bookingId,
            String reservationStatus
    ) {
        if (!StringUtils.hasText(status)) {
            return;
        }

        int incomingPriority = statusPriority(status);
        int currentPriority = statusPriority(cell.status);

        if (incomingPriority >= currentPriority) {
            cell.status = status;
            if (StringUtils.hasText(confirmationNumber)) {
                cell.confirmationNumber = confirmationNumber;
            }
            if (bookingId != null) {
                cell.bookingId = bookingId;
            }
            if (StringUtils.hasText(reservationStatus)) {
                cell.reservationStatus = reservationStatus;
            }
        }
    }

    private String resolveBookingCellStatus(String reservationStatus) {
        String normalized = normalize(reservationStatus);
        if ("ARRIVED".equals(normalized) || "CHECKED_IN".equals(normalized)) {
            return STATUS_OCCUPIED;
        }
        return STATUS_BOOKED;
    }

    private boolean isActiveReservation(String reservationStatus) {
        String normalized = normalize(reservationStatus);
        return !"CHECKED_OUT".equals(normalized)
                && !"CANCELLED".equals(normalized)
                && !"NO_SHOW".equals(normalized);
    }

    private String normalizeHousekeepingStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_AVAILABLE;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private int statusPriority(String status) {
        String normalized = normalize(status);
        return switch (normalized) {
            case STATUS_OCCUPIED -> 50;
            case STATUS_DIRTY -> 40;
            case STATUS_BOOKED -> 30;
            case STATUS_CLEANED -> 20;
            case STATUS_AVAILABLE -> 10;
            default -> 25;
        };
    }

    private List<String> sanitizeRoomTypes(List<String> roomTypes) {
        if (roomTypes == null || roomTypes.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String roomType : roomTypes) {
            if (!StringUtils.hasText(roomType)) {
                continue;
            }
            sanitized.add(roomType.trim());
        }

        if (sanitized.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(sanitized));
    }

    private Set<String> normalizeRoomTypes(List<String> roomTypes) {
        if (roomTypes == null || roomTypes.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String roomType : roomTypes) {
            String normalizedValue = normalize(roomType);
            if (StringUtils.hasText(normalizedValue)) {
                normalized.add(normalizedValue);
            }
        }

        return normalized.isEmpty() ? Set.of() : Collections.unmodifiableSet(normalized);
    }

    private boolean matchesRequestedRoomType(String sourceRoomType, Set<String> requestedRoomTypes) {
        if (requestedRoomTypes == null || requestedRoomTypes.isEmpty()) {
            return true;
        }

        String normalizedSource = normalize(sourceRoomType);
        if (!StringUtils.hasText(normalizedSource)) {
            return false;
        }

        for (String requestedRoomType : requestedRoomTypes) {
            if (normalizedSource.equals(requestedRoomType)
                    || normalizedSource.contains(requestedRoomType)
                    || requestedRoomType.contains(normalizedSource)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private List<LocalDate> buildDateRangeInclusive(LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    private LocalDate max(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate min(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private static final class RoomMeta {
        private final String roomNo;
        private String roomType;
        private Integer floor;

        private RoomMeta(String roomNo, String roomType, Integer floor) {
            this.roomNo = roomNo;
            this.roomType = roomType;
            this.floor = floor;
        }
    }

    private static final class MutableCell {
        private String status;
        private String confirmationNumber;
        private Long bookingId;
        private String reservationStatus;

        private static MutableCell available() {
            MutableCell cell = new MutableCell();
            cell.status = STATUS_AVAILABLE;
            return cell;
        }
    }

    private static final class BookingRef {
        private final Long bookingId;
        private final String reservationStatus;

        private BookingRef(Long bookingId, String reservationStatus) {
            this.bookingId = bookingId;
            this.reservationStatus = reservationStatus;
        }
    }
}
