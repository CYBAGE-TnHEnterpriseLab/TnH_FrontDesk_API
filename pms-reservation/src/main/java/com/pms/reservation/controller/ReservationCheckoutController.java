package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.service.ReservationCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations/bookings/{confirmationNumber}")
@RequiredArgsConstructor
@Tag(name = "Reservation Check-Out", description = "APIs for completing and reversing a guest check-out")
public class ReservationCheckoutController {

    private final ReservationCheckoutService reservationCheckoutService;

    @PostMapping("/check-out")
    @Operation(summary = "Complete normal check-out")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> completeCheckout(
            @PathVariable String confirmationNumber,
            @RequestParam(required = false) Long bookingId,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out completed successfully",
                bookingId == null
                        ? reservationCheckoutService.completeCheckout(confirmationNumber, request)
                        : reservationCheckoutService.completeCheckout(confirmationNumber, bookingId, request)
        ));
    }

    @PostMapping("/check-out/cancel")
    @Operation(summary = "Cancel same-day check-out and re-check in the guest")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> cancelCheckout(
            @PathVariable String confirmationNumber,
            @RequestParam(required = false) Long bookingId,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out cancelled successfully",
                bookingId == null
                        ? reservationCheckoutService.cancelCheckout(confirmationNumber, request)
                        : reservationCheckoutService.cancelCheckout(confirmationNumber, bookingId, request)
        ));
    }
}
