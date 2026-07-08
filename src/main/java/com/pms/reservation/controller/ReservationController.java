package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.service.ReservationBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Booking", description = "APIs for capturing reservation booking details")
public class ReservationController {

    private final ReservationBookingService reservationBookingService;

        @GetMapping("/payment-modes")
        @Operation(summary = "Get supported reservation payment modes",
                        description = "Returns the list of supported payment modes for reservation bookings")
        public ResponseEntity<ApiResponse<List<String>>> getPaymentModes() {
                return ResponseEntity.ok(ApiResponse.success("Payment modes fetched successfully", PaymentModes.supportedModes()));
        }

    @PostMapping("/bookings")
    @Operation(summary = "Create reservation booking",
            description = "Captures guest details and finalizes reservation with confirmation workflow")
    public ResponseEntity<ApiResponse<ReservationBookingResponseDto>> createBooking(
            @Valid @RequestBody ReservationBookingRequestDto request) {
        ReservationBookingResponseDto response = reservationBookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservation confirmed successfully", response));
    }
}
