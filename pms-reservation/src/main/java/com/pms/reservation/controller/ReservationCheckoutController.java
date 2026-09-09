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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/bookings/{confirmationNumber}")
@RequiredArgsConstructor
@Tag(name = "Reservation Check-Out", description = "APIs for completing and reversing a guest check-out")
public class ReservationCheckoutController {

    private final ReservationCheckoutService reservationCheckoutService;

    @GetMapping("/early-checkout/preview")
    @Operation(summary = "Preview early check-out charges without saving")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> previewEarlyCheckout(
            @PathVariable String confirmationNumber,
            @RequestParam(required = false) LocalDate earlyDepartureDate,
            @RequestParam(required = false) String actor
    ) {
        CheckoutRequestDto request = new CheckoutRequestDto();
        request.setActor(actor);
        request.setBusinessDate(earlyDepartureDate);
        request.setEarlyDepartureDate(earlyDepartureDate);
        return ResponseEntity.ok(ApiResponse.success(
                "Early check-out preview calculated",
                reservationCheckoutService.previewEarlyCheckout(confirmationNumber, request)
        ));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Complete normal as well as early check-out with penalty recalculation")
    public ResponseEntity<ApiResponse<CheckoutCompletionResponseDto>> completeCheckout(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out completed successfully",
                reservationCheckoutService.completeCheckout(confirmationNumber, request)
        ));
    }

    @PostMapping("/check-out/cancel")
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
