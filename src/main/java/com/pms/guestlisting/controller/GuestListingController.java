package com.pms.guestlisting.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.DepartureResponseDto;
import com.pms.guestlisting.dto.DepartureSearchRequestDto;
import com.pms.guestlisting.dto.GuestListingResponseDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.service.ArrivalService;
import com.pms.guestlisting.service.DepartureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    private final ArrivalService arrivalService;
    private final DepartureService departureService;

    @GetMapping("/list")
    @Operation(summary = "Get guest listing",
            description = "Unified retrieval API for arrivals or departures with filtering, sorting, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<GuestListingResponseDto>>> getGuestListing(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(defaultValue = VIEW_ARRIVALS)
            @Pattern(regexp = "(?i)arrivals|departures", message = "view must be arrivals or departures")
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
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") @Max(value = 100, message = "size must be <= 100") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc") String sortDir,
            @RequestParam(defaultValue = "false") Boolean includeOptions
    ) {
        String normalizedView = view == null ? VIEW_ARRIVALS : view.trim().toLowerCase();
        String resolvedSortBy = resolveSortBy(sortBy, normalizedView);

        if (VIEW_DEPARTURES.equals(normalizedView)) {
            DepartureSearchRequestDto request = new DepartureSearchRequestDto();
            request.setPropertyId(propertyId);
            request.setBusinessDate(businessDate);
            request.setSearch(search);
            request.setStatus(status);
            request.setReservationType(reservationType);
            request.setCity(city);
            request.setRoomStatus(roomStatus);
            request.setCorporateCode(corporateCode);
            request.setRoomType(roomType);
            request.setFloor(floor);
            request.setCompany(company);
            request.setSharingStatus(sharingStatus);
            request.setLoyaltyMembershipStatus(loyaltyMembershipStatus);
            request.setPage(page);
            request.setSize(size);
            request.setSortBy(resolvedSortBy);
            request.setSortDir(sortDir);
            request.setIncludeOptions(includeOptions);

            PagedResponse<DepartureResponseDto> result = departureService.searchDepartures(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Guest listing fetched successfully",
                    toUnifiedDepartureResponse(result)
            ));
        }

        ArrivalSearchRequestDto request = new ArrivalSearchRequestDto();
        request.setPropertyId(propertyId);
        request.setBusinessDate(businessDate);
        request.setSearch(search);
        request.setStatus(status);
        request.setReservationType(reservationType);
        request.setCity(city);
        request.setRoomStatus(roomStatus);
        request.setCorporateCode(corporateCode);
        request.setRoomType(roomType);
        request.setFloor(floor);
        request.setCompany(company);
        request.setSharingStatus(sharingStatus);
        request.setLoyaltyMembershipStatus(loyaltyMembershipStatus);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(resolvedSortBy);
        request.setSortDir(sortDir);
        request.setIncludeOptions(includeOptions);

        PagedResponse<ArrivalResponseDto> result = arrivalService.searchArrivals(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Guest listing fetched successfully",
                toUnifiedArrivalResponse(result)
        ));
    }

    private String resolveSortBy(String sortBy, String view) {
        if (sortBy != null && !sortBy.isBlank()) {
            return sortBy;
        }
        return VIEW_DEPARTURES.equals(view) ? "checkOutDate" : "checkInDate";
    }

    private PagedResponse<GuestListingResponseDto> toUnifiedArrivalResponse(PagedResponse<ArrivalResponseDto> source) {
        List<GuestListingResponseDto> content = source.getContent().stream()
                                .map(item -> toUnifiedArrivalItem(item, source.getBusinessDate()))
                .toList();

        return PagedResponse.<GuestListingResponseDto>builder()
                .propertyId(source.getPropertyId())
                .businessDate(source.getBusinessDate())
                .filterOptions(source.getFilterOptions())
                .content(content)
                .page(source.getPage())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .sortBy(source.getSortBy())
                .sortDir(source.getSortDir())
                .build();
    }

    private PagedResponse<GuestListingResponseDto> toUnifiedDepartureResponse(PagedResponse<DepartureResponseDto> source) {
        List<GuestListingResponseDto> content = source.getContent().stream()
                                .map(item -> toUnifiedDepartureItem(item, source.getBusinessDate()))
                .toList();

        return PagedResponse.<GuestListingResponseDto>builder()
                .propertyId(source.getPropertyId())
                .businessDate(source.getBusinessDate())
                .filterOptions(source.getFilterOptions())
                .content(content)
                .page(source.getPage())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .sortBy(source.getSortBy())
                .sortDir(source.getSortDir())
                .build();
    }

        private GuestListingResponseDto toUnifiedArrivalItem(ArrivalResponseDto item, LocalDate businessDate) {
        return GuestListingResponseDto.builder()
                .listingType("ARRIVAL")
                .id(item.getId())
                .propertyId(item.getPropertyId())
                .status(item.getStatus())
                                .dnm(isDnm(item.getStatus()))
                                .msg(false)
                .salutation(item.getSalutation())
                .firstName(item.getFirstName())
                .lastName(item.getLastName())
                .roomNo(item.getRoomNo())
                .reservationType(item.getReservationType())
                .city(item.getCity())
                .rateCode(item.getRateCode())
                                .ratePlan(item.getRateCode())
                .checkInDate(item.getCheckInDate())
                .checkOutDate(item.getCheckOutDate())
                .roomNights(item.getRoomNights())
                                .nights(item.getRoomNights())
                .roomStatus(item.getRoomStatus())
                .corporateCode(item.getCorporateCode())
                .roomType(item.getRoomType())
                .confirmationNumber(item.getConfirmationNumber())
                .company(item.getCompany())
                .sharingStatus(item.getSharingStatus())
                                .sharing(item.getSharingStatus())
                .floor(item.getFloor())
                .balance(item.getBalance())
                .loyaltyMembershipStatus(item.getLoyaltyMembershipStatus())
                                .tier(item.getLoyaltyMembershipStatus())
                                .groupCode(null)
                                .stayStatus(resolveStayStatus(businessDate, item.getCheckInDate(), item.getCheckOutDate(), "ARRIVAL"))
                .build();
    }

                    private GuestListingResponseDto toUnifiedDepartureItem(DepartureResponseDto item, LocalDate businessDate) {
        return GuestListingResponseDto.builder()
                .listingType("DEPARTURE")
                .id(item.getId())
                .propertyId(item.getPropertyId())
                .status(item.getStatus())
                                .dnm(isDnm(item.getStatus()))
                                .msg(false)
                .salutation(item.getSalutation())
                .firstName(item.getFirstName())
                .lastName(item.getLastName())
                .roomNo(item.getRoomNo())
                .reservationType(item.getReservationType())
                .city(item.getCity())
                .rateCode(item.getRateCode())
                                .ratePlan(item.getRateCode())
                .checkInDate(item.getCheckInDate())
                .checkOutDate(item.getCheckOutDate())
                .roomNights(item.getRoomNights())
                                .nights(item.getRoomNights())
                .roomStatus(item.getRoomStatus())
                .corporateCode(item.getCorporateCode())
                .roomType(item.getRoomType())
                .confirmationNumber(item.getConfirmationNumber())
                .company(item.getCompany())
                .sharingStatus(item.getSharingStatus())
                                .sharing(item.getSharingStatus())
                .floor(item.getFloor())
                .balance(item.getBalance())
                .loyaltyMembershipStatus(item.getLoyaltyMembershipStatus())
                                .tier(item.getLoyaltyMembershipStatus())
                                .groupCode(null)
                                .stayStatus(resolveStayStatus(businessDate, item.getCheckInDate(), item.getCheckOutDate(), "DEPARTURE"))
                .build();
    }

        private boolean isDnm(String status) {
                if (status == null || status.isBlank()) {
                        return false;
                }
                String normalized = status.toUpperCase(Locale.ROOT);
                return normalized.equals("DNM") || normalized.contains("DO NOT MOVE");
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
}