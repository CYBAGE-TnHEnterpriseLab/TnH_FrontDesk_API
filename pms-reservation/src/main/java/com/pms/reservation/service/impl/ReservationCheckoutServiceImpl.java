package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.service.ReservationCheckoutService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationCheckoutServiceImpl implements ReservationCheckoutService {

    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationCheckInAuditRepository auditRepository;
    private final HousekeepingRoomStatusService housekeepingRoomStatusService;

    @Override
    @Transactional
    public CheckoutCompletionResponseDto completeCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);

        if (!STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus())) {
            throw new BadRequestException("Check-out can only be initiated for a checked-in reservation");
        }
        if (!request.getBusinessDate().equals(booking.getDepartureDate())) {
            throw new BadRequestException("Check-out businessDate must match the reservation departureDate");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        booking.setReservationStatus(STATUS_CHECKED_OUT);
        booking.setCheckOutCompletedAt(completedAt);
        booking.setCheckOutCompletedBy(request.getActor().trim());
        booking.setCheckOutBusinessDate(request.getBusinessDate());
        reservationBookingRepository.save(booking);

        updateRoomStatus(booking, request, false);
        appendAudit(booking, "CHECKOUT_COMPLETED", "Guest checked out and room marked dirty", request.getActor());
        return toResponse(booking);
    }

    @Override
    @Transactional
    public CheckoutCompletionResponseDto cancelCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);

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
        return reservationBookingRepository.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
    }

    private void updateRoomStatus(ReservationBookingRecord booking, CheckoutRequestDto request, boolean occupied) {
        HousekeepingRoomStatusRequestDto housekeepingRequest = new HousekeepingRoomStatusRequestDto();
        housekeepingRequest.setPropertyId(booking.getPropertyId());
        housekeepingRequest.setBusinessDate(request.getBusinessDate());
        housekeepingRequest.setConfirmationNumber(booking.getConfirmationNumber());
        housekeepingRequest.setRoomNo(booking.getAssignedRoomNo());
        if (occupied) {
            housekeepingRoomStatusService.markOccupied(housekeepingRequest);
        } else {
            housekeepingRoomStatusService.markDirty(housekeepingRequest);
        }
    }

    private void appendAudit(ReservationBookingRecord booking, String eventType, String message, String actor) {
        auditRepository.save(ReservationCheckInAuditRecord.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .eventType(eventType)
                .eventMessage(message)
                .changedFields("reservationStatus, roomOccupancy")
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
