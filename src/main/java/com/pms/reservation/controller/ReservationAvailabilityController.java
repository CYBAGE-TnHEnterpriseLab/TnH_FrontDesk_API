package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.service.ReservationAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "arrivalDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate,
            @RequestParam(name = "departureDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @Min(value = 1, message = "night must be >= 1") Integer night,
            @RequestParam(required = false) @Min(value = 1, message = "numberOfRooms must be >= 1") Integer numberOfRooms,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) @Min(value = 1, message = "adults must be >= 1") Integer adults,
            @RequestParam(required = false) @Min(value = 0, message = "children must be >= 0") Integer children,
            @RequestParam(required = false) @Min(value = 1, message = "adultCount must be >= 1") Integer adultCount,
            @RequestParam(required = false) @Min(value = 0, message = "childCount must be >= 0") Integer childCount,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String rateCode,
            @RequestParam(required = false) String blockCode
    ) {
        LocalDate resolvedArrivalDate = date != null ? date : arrivalDate;
        if (resolvedArrivalDate == null) {
            throw new BadRequestException("date is required");
        }

        int resolvedNight;
        LocalDate resolvedDepartureDate;
        if (night != null) {
            resolvedNight = night;
            resolvedDepartureDate = resolvedArrivalDate.plusDays(night.longValue());
        } else if (arrivalDate != null && departureDate != null) {
            long computedNights = ChronoUnit.DAYS.between(arrivalDate, departureDate);
            if (computedNights < 1) {
                throw new BadRequestException("departureDate must be on or after arrivalDate");
            }
            resolvedNight = Math.toIntExact(computedNights);
            resolvedDepartureDate = departureDate;
        } else {
            throw new BadRequestException("night is required");
        }

        int resolvedNumberOfRooms = numberOfRooms == null ? 1 : numberOfRooms;
        int resolvedAdults = adults != null ? adults : (adultCount != null ? adultCount : 1);
        int resolvedChildren = children != null ? children : (childCount != null ? childCount : 0);

        ReservationAvailabilityRequestDto request = new ReservationAvailabilityRequestDto();
        request.setPropertyId(propertyId);
        request.setArrivalDate(resolvedArrivalDate);
        request.setDepartureDate(resolvedDepartureDate);
        request.setNight(resolvedNight);
        request.setNumberOfRooms(resolvedNumberOfRooms);
        request.setGroupCode(groupCode);
        request.setCompany(company);
        request.setRateCode(rateCode);
        request.setBlockCode(blockCode);
        request.setAdultCount(resolvedAdults);
        request.setChildCount(resolvedChildren);

        ReservationAvailabilityResponseDto result = reservationAvailabilityService.getAvailability(request);
        return ResponseEntity.ok(ApiResponse.success("Availability and pricing fetched successfully", result));
    }
}
