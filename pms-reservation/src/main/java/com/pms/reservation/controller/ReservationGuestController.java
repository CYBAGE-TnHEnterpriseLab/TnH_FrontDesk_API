package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.MakeReservationGuestPrimaryRequest;
import com.pms.reservation.dto.ReservationGuestAssignmentDto;
import com.pms.reservation.dto.ReservationGuestResponseDto;
import com.pms.reservation.service.ReservationGuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservation-guests")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reservation Guests", description = "Manage guest associations for reservation bookings")
public class ReservationGuestController {

    private final ReservationGuestService reservationGuestService;

    @GetMapping("/assignments")
    @Operation(
            summary = "List property-scoped guest assignments",
            description = "Returns booking/guest-profile assignments for exactly one of bookingId, "
                    + "confirmationNumber or guestProfileIds")
    public ResponseEntity<ApiResponse<List<ReservationGuestAssignmentDto>>> findGuestAssignments(
            @RequestParam @NotBlank(message = "propertyId is required") String propertyId,
            @RequestParam(required = false) @Positive(message = "bookingId must be positive") Long bookingId,
            @RequestParam(required = false) String confirmationNumber,
            @RequestParam(required = false) List<Long> guestProfileIds
    ) {
        List<ReservationGuestAssignmentDto> response = reservationGuestService.findGuestAssignments(
                propertyId, bookingId, confirmationNumber, guestProfileIds);
        return ResponseEntity.ok(ApiResponse.success("Reservation guest assignments fetched successfully", response));
    }

    @PatchMapping("/{reservationGuestId}/primary")
    @Operation(
            summary = "Make a reservation guest the primary guest",
            description = "Switches the primary guest for a booking to the specified reservation guest")
    public ResponseEntity<ApiResponse<ReservationGuestResponseDto>> makePrimaryGuest(
            @PathVariable @Positive(message = "reservationGuestId must be positive") Long reservationGuestId,
            @Valid @RequestBody MakeReservationGuestPrimaryRequest request
    ) {
        ReservationGuestResponseDto response = reservationGuestService.makePrimaryGuest(
                request.getBookingId(),
                reservationGuestId
        );
        return ResponseEntity.ok(ApiResponse.success("Primary guest updated successfully", response));
    }
}
