package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.integration.HousekeepingRoomStatusClient;
import com.pms.reservation.integration.InventoryServiceClient;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.service.ReservationCheckoutService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationCheckoutServiceImpl implements ReservationCheckoutService {

    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservationCheckoutServiceImpl.class);

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationCheckInAuditRepository auditRepository;
    private final FolioServiceClient folioServiceClient;
    private final HousekeepingRoomStatusClient housekeepingRoomStatusClient;
    private final InventoryServiceClient inventoryServiceClient;

    @Override
    @Transactional
    public CheckoutCompletionResponseDto completeCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);
        return completeCheckoutForBooking(booking, request);
    }

    @Override
    @Transactional
    public CheckoutCompletionResponseDto completeCheckout(String confirmationNumber, Long bookingId,
                                                          CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber, bookingId);
        return completeCheckoutForBooking(booking, request);
    }

    private CheckoutCompletionResponseDto completeCheckoutForBooking(ReservationBookingRecord booking,
                                                                      CheckoutRequestDto request) {

        if (!STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus())) {
            throw new BadRequestException("Check-out can only be initiated for a checked-in reservation");
        }

        BigDecimal folioBalance = folioServiceClient.getFolioBalance(
            booking.getConfirmationNumber(), booking.getId());
        if (folioBalance != null) {
            booking.setGuestBalance(folioBalance.max(BigDecimal.ZERO));
        }
        if (folioBalance != null && folioBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException(
                    "Check-out denied: folio has outstanding balance of "
                            + folioBalance.setScale(2, RoundingMode.HALF_UP)
                            + ". Please resolve the balance before checkout.");
        }

        if (folioBalance != null) {
            booking.setGuestBalance(folioBalance);
        }

        if (booking.getGuestBalance() != null && booking.getGuestBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException(
                    "Check-out denied: guest has outstanding balance of "
                            + booking.getGuestBalance().setScale(2, RoundingMode.HALF_UP)
                            + ". Please resolve the balance before checkout.");
        }

        LocalDate originalDepartureDate = booking.getDepartureDate();
        LocalDate businessDate = request.getBusinessDate();

        if (businessDate.isAfter(originalDepartureDate)) {
            throw new BadRequestException("Check-out cannot be processed after the reservation departure date");
        }

        if (!businessDate.equals(booking.getDepartureDate())) {
            throw new BadRequestException("Check-out businessDate must match the reservation departureDate");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        booking.setReservationStatus(STATUS_CHECKED_OUT);
        booking.setCheckOutCompletedAt(completedAt);
        booking.setCheckOutCompletedBy(request.getActor().trim());
        booking.setCheckOutBusinessDate(request.getBusinessDate());
        reservationBookingRepository.save(booking);

        updateRoomStatus(booking, request, false);
        inventoryServiceClient.release(booking.getConfirmationNumber());
        appendAudit(booking,
                "CHECKOUT_COMPLETED",
                "Guest checked out and room marked dirty",
                request.getActor());
        return toResponse(booking);
    }
    @Override
    @Transactional
    public CheckoutCompletionResponseDto cancelCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);
        return cancelCheckoutForBooking(booking, request);
    }

    @Override
    @Transactional
    public CheckoutCompletionResponseDto cancelCheckout(String confirmationNumber, Long bookingId,
                                                        CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber, bookingId);
        return cancelCheckoutForBooking(booking, request);
    }

    private CheckoutCompletionResponseDto cancelCheckoutForBooking(ReservationBookingRecord booking,
                                                                    CheckoutRequestDto request) {

        if (!STATUS_CHECKED_OUT.equalsIgnoreCase(booking.getReservationStatus())) {
            throw new BadRequestException("Only a checked-out reservation can have its check-out cancelled");
        }
        if (booking.getCheckOutBusinessDate() == null || !booking.getCheckOutBusinessDate().equals(request.getBusinessDate())) {
            throw new BadRequestException("Check-out can only be cancelled on the same business date it was completed");
        }

        booking.setReservationStatus(STATUS_CHECKED_IN);
        booking.setCheckOutCompletedAt(null);
        booking.setCheckOutCompletedBy(null);
        booking.setCheckOutBusinessDate(null);
        reservationBookingRepository.save(booking);

        updateRoomStatus(booking, request, true);
        appendAudit(booking, "CHECKOUT_CANCELLED", "Check-out cancelled; guest re-checked in and room marked occupied", request.getActor());
        return toResponse(booking);
    }

    private ReservationBookingRecord getBookingOrThrow(String confirmationNumber) {
        List<ReservationBookingRecord> bookings = reservationBookingRepository
            .findByConfirmationNumber(confirmationNumber);
        if (bookings.size() > 1) {
            throw new BadRequestException("bookingId is required when a confirmation has multiple rooms");
        }
        return bookings.stream().findFirst()
            .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
    }

    private ReservationBookingRecord getBookingOrThrow(String confirmationNumber, Long bookingId) {
        return reservationBookingRepository.findByIdAndConfirmationNumber(bookingId, confirmationNumber)
                .orElseThrow(() -> new BadRequestException("Reservation room booking not found"));
    }

    private void updateRoomStatus(ReservationBookingRecord booking, CheckoutRequestDto request, boolean occupied) {
        if (booking.getAssignedRoomNo() == null) {
            return;
        }
        try {
            UUID propertyId = UUID.fromString(booking.getPropertyId());
            if (occupied) {
                housekeepingRoomStatusClient.updateCheckedInStatus(
                        propertyId,
                        request.getBusinessDate(),
                        booking.getArrivalDate(),
                        booking.getDepartureDate(),
                        booking.getAssignedRoomNo(),
                        booking.getGuestName(),
                        booking.getConfirmationNumber()
                );
            } else {
                housekeepingRoomStatusClient.markRoomDirty(
                        propertyId,
                        request.getBusinessDate(),
                        booking.getArrivalDate(),
                        booking.getDepartureDate(),
                        booking.getAssignedRoomNo()
                );
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping housekeeping room status update for confirmation {} because propertyId is not a UUID: {}",
                    booking.getConfirmationNumber(), booking.getPropertyId());
        }
    }

    private void appendAudit(ReservationBookingRecord booking, String eventType, String message, String actor) {
        auditRepository.save(ReservationCheckInAuditRecord.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .eventType(eventType)
                .eventMessage(message)
                .changedFields("reservationStatus, roomOccupancy, departureDate, totalRate")
                .actor(StringUtils.hasText(actor) ? actor.trim() : "system")
                .build());
    }

    private CheckoutCompletionResponseDto toResponse(ReservationBookingRecord booking) {
        return CheckoutCompletionResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .reservationStatus(booking.getReservationStatus())
                .businessDate(booking.getCheckOutBusinessDate())
                .checkOutCompletedAt(booking.getCheckOutCompletedAt())
                .checkOutCompletedBy(booking.getCheckOutCompletedBy())
                .build();
    }
}
