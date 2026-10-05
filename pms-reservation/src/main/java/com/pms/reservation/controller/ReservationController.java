package com.pms.reservation.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.constant.PaymentTypes;
import com.pms.reservation.constant.IdTypes;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.dto.HousekeepingSyncResponse;
import com.pms.reservation.service.ReservationBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

        @GetMapping("/id-types")
        @Operation(summary = "Get supported guest identity types",
                description = "Returns the identity proof types available when capturing guest details")
        public ResponseEntity<ApiResponse<List<String>>> getIdTypes() {
            return ResponseEntity.ok(ApiResponse.success("Identity types fetched successfully", IdTypes.supportedTypes()));
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

        @GetMapping("/bookings/{confirmationNumber}")
        @Operation(summary = "Get reservation details",
                        description = "Returns detailed reservation payload for reservation view UI using confirmation number")
        public ResponseEntity<ApiResponse<ReservationViewResponseDto>> getBookingDetails(@PathVariable String confirmationNumber) {
                ReservationViewResponseDto response = reservationBookingService.getBookingDetails(confirmationNumber);
                return ResponseEntity.ok(ApiResponse.success("Reservation fetched successfully", response));
        }

                @PatchMapping("/bookings/{confirmationNumber}/guest-balance")
                public ResponseEntity<Void> updateGuestBalance(
                    @PathVariable String confirmationNumber,
                    @RequestParam(required = false) Long bookingId,
                    @RequestParam BigDecimal guestBalance) {
                    reservationBookingService.updateGuestBalance(confirmationNumber, bookingId, guestBalance);
                    return ResponseEntity.noContent().build();
                }

            @GetMapping("/bookings/search")
            @Operation(summary = "Search reservation booking",
                    description = "Returns all room bookings under the matched confirmation number using confirmation number, booking ID, phone number, or email")
            public ResponseEntity<ApiResponse<ReservationViewResponseDto>> searchBooking(
                @RequestParam(required = false) String confirmationNumber,
                @RequestParam(required = false) Long bookingId,
                @RequestParam(required = false) String propertyId,
                @RequestParam(required = false) String phoneNumber,
                @RequestParam(required = false) String email) {
                ReservationViewResponseDto response = reservationBookingService.searchBooking(
                    confirmationNumber, bookingId, propertyId, phoneNumber, email);
                return ResponseEntity.ok(ApiResponse.success("Reservation fetched successfully", response));
            }

        @GetMapping("/bookings/{confirmationNumber}/rooms/{bookingId}")
        @Operation(summary = "Get one room booking",
            description = "Returns one room booking within a shared confirmation number")
        public ResponseEntity<ApiResponse<ReservationViewResponseDto>> getRoomBookingDetails(
            @PathVariable String confirmationNumber,
            @PathVariable Long bookingId) {
            ReservationViewResponseDto response = reservationBookingService.getBookingDetails(
                confirmationNumber, bookingId);
            return ResponseEntity.ok(ApiResponse.success("Room booking fetched successfully", response));
        }

        @PatchMapping("/bookings/{confirmationNumber}")
    @Operation(summary = "Edit reservation booking",
            description = "Updates an existing reservation booking by confirmation number")
    public ResponseEntity<ApiResponse<ReservationViewResponseDto>> updateBooking(
            @PathVariable String confirmationNumber,
            @RequestBody ReservationBookingRequestDto request) {
        ReservationViewResponseDto response = reservationBookingService.updateBooking(confirmationNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Reservation updated successfully", response));
    }

        @PatchMapping("/bookings/{confirmationNumber}/rooms/{bookingId}")
        @Operation(summary = "Edit an individual room booking",
            description = "Updates one booking row within a shared confirmation number")
        public ResponseEntity<ApiResponse<ReservationViewResponseDto>> updateRoomBooking(
            @PathVariable String confirmationNumber,
            @PathVariable Long bookingId,
            @RequestBody ReservationBookingRequestDto request) {
        ReservationViewResponseDto response = reservationBookingService.updateBooking(
            confirmationNumber, bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Room booking updated successfully", response));
        }

    @GetMapping("/bookings")
    @Operation(summary = "Get all reservation bookings",
            description = "Returns all created reservation bookings ordered by latest first")
    public ResponseEntity<ApiResponse<List<ReservationBookingResponseDto>>> getBookings() {
        List<ReservationBookingResponseDto> response = reservationBookingService.getBookings();
        return ResponseEntity.ok(ApiResponse.success("Reservations fetched successfully", response));
    }

    @PostMapping("/housekeeping/sync")
    @Operation(summary = "Synchronize confirmed reservations with Housekeeping")
    public ResponseEntity<ApiResponse<HousekeepingSyncResponse>> syncHousekeepingStatuses() {
        HousekeepingSyncResponse response = reservationBookingService.syncHousekeepingStatuses();
        return ResponseEntity.ok(ApiResponse.success("Housekeeping synchronization completed", response));
    }

    @GetMapping("/payment-types")
    @Operation(summary = "Get supported reservation payment types",
            description = "Returns the list of supported payment types for reservation bookings")
    public ResponseEntity<ApiResponse<List<String>>> getPaymentTypes() {
        return ResponseEntity.ok(ApiResponse.success("Payment types fetched successfully", PaymentTypes.supportedTypes()));
    }
}
