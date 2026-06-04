package com.hotel.pms.frontdesk.guestlisting.controller;

import com.hotel.pms.frontdesk.guestlisting.dto.ApiResponse;
import com.hotel.pms.frontdesk.guestlisting.dto.DepartureResponseDto;
import com.hotel.pms.frontdesk.guestlisting.dto.DepartureSearchRequestDto;
import com.hotel.pms.frontdesk.guestlisting.dto.PagedResponse;
import com.hotel.pms.frontdesk.guestlisting.service.DepartureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departures")
@RequiredArgsConstructor
@Tag(name = "Departure Screen", description = "APIs for hotel front desk departures")
public class DepartureController {

    private final DepartureService departureService;

    @GetMapping("/list")
    @Operation(summary = "Get departures",
            description = "Canonical retrieval API with backend filtering, sorting, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<DepartureResponseDto>>> getDepartures(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reservationType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String roomStatus,
            @RequestParam(required = false) String corporateCode,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String sharingStatus,
            @RequestParam(required = false) String loyaltyMembershipStatus,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be >= 0") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be >= 1") @Max(value = 100, message = "size must be <= 100") Integer size,
            @RequestParam(defaultValue = "checkOutDate") String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc") String sortDir,
            @RequestParam(defaultValue = "false") Boolean includeOptions
    ) {
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
        request.setCompany(company);
        request.setSharingStatus(sharingStatus);
        request.setLoyaltyMembershipStatus(loyaltyMembershipStatus);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDir(sortDir);
        request.setIncludeOptions(includeOptions);

        PagedResponse<DepartureResponseDto> result = departureService.searchDepartures(request);
        return ResponseEntity.ok(ApiResponse.success("Departure list fetched successfully", result));
    }
}
