package com.pms.guestlisting.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ArrivalSearchRequestDto;
import com.pms.guestlisting.dto.PagedResponse;
import com.pms.guestlisting.service.ArrivalService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/arrivals")
@RequiredArgsConstructor
@Tag(name = "Arrival Screen", description = "APIs for hotel front desk arrivals")
@Validated
public class ArrivalController {

    private final ArrivalService arrivalService;

        @GetMapping("/list")
        @Operation(summary = "Get arrivals",
                        description = "Canonical retrieval API with backend filtering, sorting, and pagination")
        public ResponseEntity<ApiResponse<PagedResponse<ArrivalResponseDto>>> getArrivals(
                        @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
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
                        @RequestParam(defaultValue = "checkInDate") String sortBy,
                        @RequestParam(defaultValue = "asc") @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc") String sortDir,
                        @RequestParam(defaultValue = "false") Boolean includeOptions
        ) {
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
                request.setSortBy(sortBy);
                request.setSortDir(sortDir);
                request.setIncludeOptions(includeOptions);

                PagedResponse<ArrivalResponseDto> result = arrivalService.searchArrivals(request);
                return ResponseEntity.ok(ApiResponse.success("Arrival list fetched successfully", result));
        }
}

