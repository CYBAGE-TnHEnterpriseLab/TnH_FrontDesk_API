package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.service.ReservationCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/bookings/{confirmationNumber}/check-out")
@RequiredArgsConstructor
@Tag(name = "Reservation Check-Out", description = "APIs for completing and reversing a guest check-out")
public class ReservationCheckoutController {

    private final ReservationCheckoutService reservationCheckoutService;

    @PostMapping
    @Operation(summary = "Complete check-out")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> completeCheckout(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out completed successfully",
                reservationCheckoutService.completeCheckout(confirmationNumber, request)
        ));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel same-day check-out and re-check in the guest")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> cancelCheckout(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out cancelled successfully",
                reservationCheckoutService.cancelCheckout(confirmationNumber, request)
        ));
    }
}
