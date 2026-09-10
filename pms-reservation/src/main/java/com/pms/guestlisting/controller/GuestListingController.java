package com.pms.guestlisting.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.guestlisting.dto.FilterOptionsDto;
import com.pms.guestlisting.dto.GuestListingResponseDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest-listing")
@RequiredArgsConstructor
@Tag(name = "Guest Listing", description = "Unified API for front desk guest listing")
@Validated
public class GuestListingController {

    private static final String VIEW_ARRIVALS = "arrivals";
    private static final String VIEW_DEPARTURES = "departures";
    private static final String VIEW_ALL = "all";
    private static final int FIXED_PAGE_SIZE = 10;
    private static final Set<String> SUPPORTED_ROOM_STATUSES = Set.of("OCCUPIED", "DIRTY", "CLEANED");
        private static final String STATUS_CONFIRMED = "CONFIRMED";
        private static final String STATUS_NO_SHOW = "NO_SHOW";
        private static final Set<String> SUPPORTED_RESERVATION_STATUSES = Set.of(
            STATUS_CONFIRMED, "CHECKED_IN", "CHECKED_OUT", STATUS_NO_SHOW);

    private record RoomStatusSnapshot(String roomStatus, String roomNo) {
    }

    private final ReservationBookingRepository reservationBookingRepository;
    private final HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @GetMapping("/list")
    @Operation(summary = "Get guest listing",
            description = "Unified retrieval API for arrivals/departures/all reservations with filtering, sorting, and pagination")
        @Transactional
    public ResponseEntity<ApiResponse<PagedResponse<GuestListingResponseDto>>> getGuestListing(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false)
            @Pattern(regexp = "(?i)arrivals|departures|all", message = "view must be arrivals, departures or all")
            String view,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reservationType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String roomStatus,
            @RequestParam(required = false) String corporateCode,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String sharingStatus,
            @RequestParam(required = false) String loyaltyMembershipStatus,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") Integer page,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc") String sortDir,
            @RequestParam(defaultValue = "false") Boolean includeOptions
    ) {
        reservationBookingRepository.markPastConfirmedReservationsAsNoShow(propertyId, businessDate);

        String normalizedView = normalizeView(view);
        String resolvedSortBy = resolveSortBy(sortBy, normalizedView);
        String normalizedRoomStatus = normalizeRoomStatus(roomStatus);
        Set<String> confirmationNumberFilter = resolveConfirmationNumberFilter(
            propertyId,
            businessDate,
            normalizedRoomStatus
        );

        if (confirmationNumberFilter != null && confirmationNumberFilter.isEmpty()) {
            PagedResponse<GuestListingResponseDto> emptyResponse = PagedResponse.<GuestListingResponseDto>builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
                .filterOptions(Boolean.TRUE.equals(includeOptions)
                    ? buildFilterOptions(propertyId, businessDate, normalizedView)
                    : null)
                .content(List.of())
                .page(page)
                .size(FIXED_PAGE_SIZE)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .sortBy(resolvedSortBy)
                .sortDir(sortDir)
                .build();
            return ResponseEntity.ok(ApiResponse.success("Guest listing fetched successfully", emptyResponse));
        }

        Pageable pageable = PageRequest.of(page, FIXED_PAGE_SIZE, buildSort(resolvedSortBy, sortDir));
        Page<ReservationBookingRecord> bookingPage = reservationBookingRepository.findAll(
            byCriteria(
                propertyId,
                businessDate,
                normalizedView,
                search,
                status,
                reservationType,
                city,
            normalizedRoomStatus,
                corporateCode,
                roomType,
                floor,
                company,
                sharingStatus,
            loyaltyMembershipStatus,
            confirmationNumberFilter
            ),
                pageable
        );

        Map<String, RoomStatusSnapshot> roomStatusByConfirmation = loadRoomStatusByConfirmation(
            propertyId,
            businessDate,
            bookingPage.getContent().stream()
                .map(ReservationBookingRecord::getConfirmationNumber)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet())
        );

        List<GuestListingResponseDto> content = bookingPage.getContent().stream()
            .map(booking -> toGuestListingItem(
                booking,
                businessDate,
                normalizedView,
                roomStatusByConfirmation.get(booking.getConfirmationNumber())
            ))
                .toList();

        PagedResponse<GuestListingResponseDto> response = PagedResponse.<GuestListingResponseDto>builder()
                .propertyId(propertyId)
                .businessDate(businessDate)
            .filterOptions(Boolean.TRUE.equals(includeOptions)
                ? buildFilterOptions(propertyId, businessDate, normalizedView)
                : null)
                .content(content)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .first(bookingPage.isFirst())
                .last(bookingPage.isLast())
                .sortBy(resolvedSortBy)
                .sortDir(sortDir)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Guest listing fetched successfully", response));
    }

    private FilterOptionsDto buildFilterOptions(String propertyId, LocalDate businessDate, String view) {
        List<ReservationBookingRecord> records = reservationBookingRepository.findAll(
            byCriteria(propertyId, businessDate, view, null, null, null, null, null, null, null, null, null, null, null, null)
        );

        Map<String, RoomStatusSnapshot> roomStatusByConfirmation = loadRoomStatusByConfirmation(
                propertyId,
                businessDate,
                records.stream()
                        .map(ReservationBookingRecord::getConfirmationNumber)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toSet())
        );

        Set<String> reservationStatuses = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> roomStatuses = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> stayTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> roomTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> loyalties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> vips = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<Integer> floors = new TreeSet<>(Comparator.nullsLast(Integer::compareTo));

        for (ReservationBookingRecord record : records) {
            String reservationStatus = resolveReservationStatus(record, businessDate);
            if (StringUtils.hasText(reservationStatus)
                    && SUPPORTED_RESERVATION_STATUSES.contains(reservationStatus.toUpperCase(Locale.ROOT))) {
                reservationStatuses.add(reservationStatus);
            }

            if (StringUtils.hasText(record.getReservationType())) {
                stayTypes.add(record.getReservationType());
            }
            if (StringUtils.hasText(record.getRoomType())) {
                roomTypes.add(record.getRoomType());
            }
            if (StringUtils.hasText(record.getLoyaltyNumber())) {
                loyalties.add(record.getLoyaltyNumber());
            }
            if (record.getVipTag() != null) {
                vips.add(Boolean.TRUE.equals(record.getVipTag()) ? "Y" : "N");
            }
            if (record.getFloor() != null) {
                floors.add(record.getFloor());
            }

            RoomStatusSnapshot roomSnapshot = roomStatusByConfirmation.get(record.getConfirmationNumber());
            if (roomSnapshot != null && StringUtils.hasText(roomSnapshot.roomStatus())) {
                roomStatuses.add(roomSnapshot.roomStatus());
            }
        }

        return FilterOptionsDto.builder()
                .reservationStatuses(List.copyOf(reservationStatuses))
                .roomStatuses(List.copyOf(roomStatuses))
                .stayTypes(List.copyOf(stayTypes))
                .roomTypes(List.copyOf(roomTypes))
                .floors(List.copyOf(floors))
                .loyalties(List.copyOf(loyalties))
                .vips(vips.isEmpty() ? List.of("Y", "N") : List.copyOf(vips))
                .build();
    }

    private Specification<ReservationBookingRecord> byCriteria(
            String propertyId,
            LocalDate businessDate,
            String view,
            String search,
            String status,
            String reservationType,
            String city,
            String roomStatus,
            String corporateCode,
            String roomType,
            Integer floor,
            String company,
            String sharingStatus,
            String loyaltyMembershipStatus,
            Set<String> confirmationNumberFilter
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("propertyId"), propertyId));

            if (confirmationNumberFilter != null) {
                if (confirmationNumberFilter.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("confirmationNumber").in(confirmationNumberFilter));
                }
            }

            if (VIEW_ARRIVALS.equals(view)) {
                predicates.add(cb.equal(root.get("arrivalDate"), businessDate));
            } else if (VIEW_DEPARTURES.equals(view)) {
                predicates.add(cb.equal(root.get("departureDate"), businessDate));
            } else {
                predicates.add(cb.or(
                        cb.equal(root.get("arrivalDate"), businessDate),
                        cb.equal(root.get("departureDate"), businessDate)
                ));
            }

            if (StringUtils.hasText(status)) {
                String normalizedStatus = status.toUpperCase(Locale.ROOT);
                if (STATUS_NO_SHOW.equals(normalizedStatus)) {
                    predicates.add(cb.and(
                            cb.equal(cb.upper(root.get("reservationStatus")), STATUS_CONFIRMED),
                            cb.lessThan(root.get("arrivalDate"), businessDate)
                    ));
                } else {
                    Predicate storedStatus = cb.equal(cb.lower(root.get("reservationStatus")), status.toLowerCase(Locale.ROOT));
                    if (STATUS_CONFIRMED.equals(normalizedStatus)) {
                        storedStatus = cb.and(storedStatus, cb.greaterThanOrEqualTo(root.get("arrivalDate"), businessDate));
                    }
                    predicates.add(storedStatus);
                }
            }
            if (StringUtils.hasText(reservationType)) {
                predicates.add(cb.equal(cb.lower(root.get("reservationType")), reservationType.toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(city)) {
                predicates.add(cb.like(cb.lower(root.get("city")), like(city)));
            }
            if (StringUtils.hasText(roomStatus)) {
                // Filter is handled through confirmationNumberFilter from housekeeping statuses.
            }
            if (StringUtils.hasText(corporateCode)) {
                // corporate code isn't present in reservation_bookings; map to guestGroup as closest available field.
                predicates.add(cb.like(cb.lower(root.get("guestGroup")), like(corporateCode)));
            }
            if (StringUtils.hasText(roomType)) {
                predicates.add(cb.like(cb.lower(root.get("roomType")), like(roomType)));
            }
            if (floor != null) {
                predicates.add(cb.equal(root.get("floor"), floor));
            }
            if (StringUtils.hasText(company)) {
                predicates.add(cb.like(cb.lower(root.get("company")), like(company)));
            }
            if (StringUtils.hasText(sharingStatus)) {
                try {
                    predicates.add(cb.equal(root.get("numberOfRooms"), Integer.parseInt(sharingStatus.trim())));
                } catch (NumberFormatException ignored) {
                    predicates.add(cb.disjunction());
                }
            }
            if (StringUtils.hasText(loyaltyMembershipStatus)) {
                predicates.add(cb.like(cb.lower(root.get("loyaltyNumber")), like(loyaltyMembershipStatus)));
            }

            if (StringUtils.hasText(search)) {
                String wildcard = like(search);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("guestName")), wildcard),
                        cb.like(cb.lower(root.get("confirmationNumber")), wildcard),
                        cb.like(cb.lower(root.get("company")), wildcard),
                        cb.like(cb.lower(root.get("city")), wildcard),
                        cb.like(cb.lower(root.get("roomType")), wildcard)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizeView(String view) {
        if (!StringUtils.hasText(view)) {
            return VIEW_ALL;
        }
        return view.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSortBy(String sortBy, String view) {
        if (StringUtils.hasText(sortBy)) {
            return sortBy;
        }
        if (VIEW_DEPARTURES.equals(view)) {
            return "checkOutDate";
        }
        return "checkInDate";
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String mapped = mapSortProperty(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        if ("guestName".equals(sortBy)) {
            return Sort.by(new Sort.Order(direction, "guestName"), new Sort.Order(direction, "confirmationNumber"));
        }
        return Sort.by(direction, mapped);
    }

    private String mapSortProperty(String sortBy) {
        return switch (sortBy) {
            case "checkOutDate" -> "departureDate";
            case "checkInDate" -> "arrivalDate";
            case "firstName", "lastName", "guestName" -> "guestName";
            case "confirmationNumber" -> "confirmationNumber";
            case "roomType" -> "roomType";
            case "reservationType" -> "reservationType";
            case "city" -> "city";
            case "company" -> "company";
            default -> "arrivalDate";
        };
    }

    private GuestListingResponseDto toGuestListingItem(ReservationBookingRecord booking, LocalDate businessDate, String view) {
        return toGuestListingItem(booking, businessDate, view, null);
        }

        private GuestListingResponseDto toGuestListingItem(
            ReservationBookingRecord booking,
            LocalDate businessDate,
            String view,
            RoomStatusSnapshot roomSnapshot
        ) {
        String[] names = splitGuestName(booking.getGuestName());
        String listingType = resolveListingType(booking, businessDate, view);
        Integer roomNights = resolveNights(booking.getArrivalDate(), booking.getDepartureDate());

        return GuestListingResponseDto.builder()
                .listingType(listingType)
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .status(resolveReservationStatus(booking, businessDate))
                .dnm(Boolean.TRUE.equals(booking.getDnm()))
                .msg(false)
                .salutation(booking.getSalutation())
                .firstName(names[0])
                .lastName(names[1])
                .roomNo(resolveRoomNo(booking, roomSnapshot))
                .reservationType(booking.getReservationType())
                .city(booking.getCity())
                .rateCode(booking.getRateCode())
                .ratePlan(booking.getRateCode())
                .guests(resolveGuestCount(booking.getAdultCount(), booking.getChildCount()))
                .checkInDate(booking.getArrivalDate())
                .checkOutDate(booking.getDepartureDate())
                .nights(roomNights)
                .roomStatus(roomSnapshot == null ? null : roomSnapshot.roomStatus())
                .corporateCode(null)
                .roomType(booking.getRoomType())
                .confirmationNumber(booking.getConfirmationNumber())
                .company(booking.getCompany())
                .sharingStatus(String.valueOf(booking.getNumberOfRooms()))
                .sharing(String.valueOf(booking.getNumberOfRooms()))
                .floor(booking.getFloor())
                .balance(booking.getGuestBalance() == null ? BigDecimal.ZERO : booking.getGuestBalance())
                .loyaltyMembershipStatus(booking.getLoyaltyNumber())
                .tier(booking.getLoyaltyNumber())
                .groupCode(booking.getGuestGroup())
                .stayStatus(resolveStayStatus(businessDate, booking.getArrivalDate(), booking.getDepartureDate(), listingType))
                .build();
    }

    private String resolveReservationStatus(ReservationBookingRecord booking, LocalDate businessDate) {
        if (STATUS_CONFIRMED.equalsIgnoreCase(booking.getReservationStatus())
                && businessDate != null
                && booking.getArrivalDate() != null
                && businessDate.isAfter(booking.getArrivalDate())) {
            return STATUS_NO_SHOW;
        }
        return booking.getReservationStatus();
    }

    private String resolveListingType(ReservationBookingRecord booking, LocalDate businessDate, String view) {
        if (VIEW_ARRIVALS.equals(view)) {
            return "ARRIVAL";
        }
        if (VIEW_DEPARTURES.equals(view)) {
            return "DEPARTURE";
        }
        if (businessDate != null && businessDate.equals(booking.getArrivalDate())) {
            return "ARRIVAL";
        }
        if (businessDate != null && businessDate.equals(booking.getDepartureDate())) {
            return "DEPARTURE";
        }
        return "RESERVATION";
    }

    private Integer resolveNights(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        return (int) Math.max(1, days);
    }

    private Integer resolveGuestCount(Integer adultCount, Integer childCount) {
        int adults = adultCount == null ? 0 : Math.max(0, adultCount);
        int children = childCount == null ? 0 : Math.max(0, childCount);
        return adults + children;
    }

    private String resolveRoomNo(ReservationBookingRecord booking, RoomStatusSnapshot roomSnapshot) {
        if (roomSnapshot != null && StringUtils.hasText(roomSnapshot.roomNo())) {
            return roomSnapshot.roomNo();
        }
        return booking.getAssignedRoomNo();
    }

    private String resolveStayStatus(LocalDate businessDate, LocalDate checkInDate, LocalDate checkOutDate, String listingType) {
        if (businessDate == null || checkInDate == null || checkOutDate == null) {
            return null;
        }
        if (businessDate.equals(checkInDate)) {
            return "ARRIVING";
        }
        if (businessDate.equals(checkOutDate)) {
            return "DEPARTING";
        }
        if (!businessDate.isBefore(checkInDate) && businessDate.isBefore(checkOutDate)) {
            return "IN_HOUSE";
        }
        if (businessDate.isBefore(checkInDate)) {
            return "BOOKED";
        }
        if (businessDate.isAfter(checkOutDate)) {
            return "CHECKED_OUT";
        }
        return "ARRIVAL".equals(listingType) ? "ARRIVING" : "DEPARTING";
    }

    private String[] splitGuestName(String guestName) {
        if (!StringUtils.hasText(guestName)) {
            return new String[] {"Guest", ""};
        }
        String[] parts = guestName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new String[] {parts[0], ""};
        }
        return parts;
    }

    private String like(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeRoomStatus(String roomStatus) {
        if (!StringUtils.hasText(roomStatus)) {
            return null;
        }
        String normalized = roomStatus.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROOM_STATUSES.contains(normalized)) {
            throw new BadRequestException("roomStatus must be OCCUPIED, DIRTY, or CLEANED");
        }
        return normalized;
    }

    private Set<String> resolveConfirmationNumberFilter(String propertyId, LocalDate businessDate, String roomStatus) {
        if (!StringUtils.hasText(roomStatus)) {
            return null;
        }

        List<String> confirmations = housekeepingRoomStatusRepository
                .findConfirmationNumbersByPropertyIdAndBusinessDateAndRoomStatus(propertyId, businessDate, roomStatus);
        if (confirmations == null) {
            return Set.of();
        }
        return new HashSet<>(confirmations);
    }

        private Map<String, RoomStatusSnapshot> loadRoomStatusByConfirmation(
            String propertyId,
            LocalDate businessDate,
            Set<String> confirmationNumbers
    ) {
        if (confirmationNumbers == null || confirmationNumbers.isEmpty()) {
            return Map.of();
        }

        List<HousekeepingRoomStatusRecord> statuses = housekeepingRoomStatusRepository
                .findByPropertyIdAndBusinessDateAndConfirmationNumberIn(propertyId, businessDate, confirmationNumbers);

        if (statuses == null || statuses.isEmpty()) {
            return Map.of();
        }

        return statuses.stream()
                .filter(status -> StringUtils.hasText(status.getConfirmationNumber()))
                .collect(Collectors.toMap(
                        HousekeepingRoomStatusRecord::getConfirmationNumber,
                status -> new RoomStatusSnapshot(status.getRoomStatus(), status.getRoomNo()),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
    }
}
