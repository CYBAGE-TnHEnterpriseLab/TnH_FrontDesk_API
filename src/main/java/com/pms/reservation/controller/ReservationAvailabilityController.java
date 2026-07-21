package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.service.ReservationAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reservation Availability", description = "APIs for live room availability and dynamic pricing")
public class ReservationAvailabilityController {

    private final ReservationAvailabilityService reservationAvailabilityService;

    @GetMapping("/availability")
    @Operation(summary = "Get room availability and pricing",
            description = "Returns live inventory and dynamic pricing based on selected stay details")
    public ResponseEntity<ApiResponse<ReservationAvailabilityResponseDto>> getAvailability(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @Min(value = 1, message = "night must be >= 1") Integer night,
            @RequestParam @Min(value = 1, message = "numberOfRooms must be >= 1") Integer numberOfRooms,
            @RequestParam(required = false) String groupCode,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "adults must be >= 1") Integer adults,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "children must be >= 0") Integer children,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String rateCode,
            @RequestParam(required = false) String blockCode
    ) {
        LocalDate resolvedDepartureDate = date.plusDays(night.longValue());

        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId(propertyId);
        request.setArrivalDate(date);
        request.setDepartureDate(resolvedDepartureDate);
        request.setNight(night);
        request.setNumberOfRooms(numberOfRooms);
        request.setGroupCode(groupCode);
        request.setCompany(company);
        request.setRateCode(rateCode);
        request.setBlockCode(blockCode);
        request.setAdultCount(adults);
        request.setChildCount(children);

        ReservationAvailabilityResponseDto result = reservationAvailabilityService.getAvailability(request);
        return ResponseEntity.ok(ApiResponse.success("Availability and pricing fetched successfully", result));
    }
}
