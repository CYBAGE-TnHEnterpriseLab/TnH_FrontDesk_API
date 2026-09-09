package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.integration.FolioServiceClient;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.service.ReservationCheckoutService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final FolioServiceClient folioServiceClient;

    @Override
    @Transactional
    public CheckoutCompletionResponseDto completeCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);

        if (!STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus())) {
            throw new BadRequestException("Check-out can only be initiated for a checked-in reservation");
        }

        LocalDate originalDepartureDate = booking.getDepartureDate();
        LocalDate businessDate = request.getBusinessDate();
        LocalDate requestedEarlyDeparture = request.getEarlyDepartureDate();

        if (businessDate.isAfter(originalDepartureDate)) {
            throw new BadRequestException("Check-out cannot be processed after the reservation departure date");
        }

        boolean earlyCheckout = false;
        if (businessDate.isBefore(originalDepartureDate)) {
            if (requestedEarlyDeparture == null) {
                throw new BadRequestException("earlyDepartureDate is required for early checkout");
            }
            if (!requestedEarlyDeparture.equals(businessDate)) {
                throw new BadRequestException("For early checkout, earlyDepartureDate must match businessDate");
            }
            earlyCheckout = true;
        } else {
            earlyCheckout = requestedEarlyDeparture != null
                    && !requestedEarlyDeparture.equals(originalDepartureDate);
        }

        BigDecimal refundAmount = null;
        if (earlyCheckout) {
            validateEarlyDeparture(booking, requestedEarlyDeparture);
            long originalNights = ChronoUnit.DAYS.between(booking.getArrivalDate(), originalDepartureDate);
            BigDecimal originalTotalRate = booking.getTotalRate();
            booking.setDepartureDate(requestedEarlyDeparture);
            recalculateBookingCharges(booking, originalNights);

            refundAmount = originalTotalRate.subtract(booking.getTotalRate());
            if (booking.getGuestBalance() != null) {
                booking.setGuestBalance(booking.getGuestBalance().subtract(refundAmount));
            }
        }

        if (!businessDate.equals(booking.getDepartureDate())) {
            throw new BadRequestException("Check-out businessDate must match the reservation departureDate");
        }

        BigDecimal folioBalance = folioServiceClient.getFolioBalance(confirmationNumber);
        if (folioBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Check-out denied: folio has outstanding balance of " + folioBalance);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        booking.setReservationStatus(STATUS_CHECKED_OUT);
        booking.setCheckOutCompletedAt(completedAt);
        booking.setCheckOutCompletedBy(request.getActor().trim());
        booking.setCheckOutBusinessDate(request.getBusinessDate());
        reservationBookingRepository.save(booking);

        updateRoomStatus(booking, request, false);
        appendAudit(booking,
                earlyCheckout ? "EARLY_CHECKOUT_COMPLETED" : "CHECKOUT_COMPLETED",
                earlyCheckout ? "Guest checked out early; room charges recalculated" : "Guest checked out and room marked dirty",
                request.getActor());
        return toResponse(booking, earlyCheckout ? requestedEarlyDeparture : null, refundAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutCompletionResponseDto previewEarlyCheckout(String confirmationNumber, CheckoutRequestDto request) {
        ReservationBookingRecord booking = getBookingOrThrow(confirmationNumber);

        if (!STATUS_CHECKED_IN.equalsIgnoreCase(booking.getReservationStatus())) {
            throw new BadRequestException("Early check-out preview can only be generated for a checked-in reservation");
        }

        LocalDate requestedEarlyDeparture = request.getEarlyDepartureDate();
        if (requestedEarlyDeparture == null) {
            throw new BadRequestException("earlyDepartureDate is required for early check-out preview");
        }

        validateEarlyDeparture(booking, requestedEarlyDeparture);

        long originalNights = ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate());
        long newNights = ChronoUnit.DAYS.between(booking.getArrivalDate(), requestedEarlyDeparture);

        BigDecimal recalculatedTotalRate = calculateRecalculatedRate(booking, requestedEarlyDeparture, originalNights);
        BigDecimal refundAmount = booking.getTotalRate().subtract(recalculatedTotalRate);

        return CheckoutCompletionResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .reservationStatus(booking.getReservationStatus())
                .businessDate(booking.getCheckOutBusinessDate())
                .checkOutCompletedAt(null)
                .checkOutCompletedBy(null)
                .earlyDepartureDate(requestedEarlyDeparture)
                .recalculatedTotalRate(recalculatedTotalRate)
                .updatedGuestBalance(
                        booking.getGuestBalance() != null
                                ? booking.getGuestBalance().subtract(refundAmount)
                                : null)
                .refundAmount(refundAmount)
                .message(String.format(
                        "Early check-out on %s will change stay from %d to %d nights. 1-day penalty applied. Refund: %s%s",
                        requestedEarlyDeparture,
                        originalNights,
                        newNights,
                        refundAmount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "",
                        refundAmount.abs().setScale(2, RoundingMode.HALF_UP)))
                .build();
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
        return toResponse(booking, null, null);
    }

    private ReservationBookingRecord getBookingOrThrow(String confirmationNumber) {
        return reservationBookingRepository.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
    }

    private void validateEarlyDeparture(ReservationBookingRecord booking, LocalDate earlyDepartureDate) {
        if (earlyDepartureDate == null) {
            return;
        }
//        if (!earlyDepartureDate.isAfter(booking.getArrivalDate())) {
//            throw new BadRequestException("Early departure date must be after arrival date");
//        }
        if (earlyDepartureDate.isAfter(booking.getDepartureDate())) {
            throw new BadRequestException("Early departure date cannot be after the scheduled departure date");
        }
    }

    private BigDecimal computeRecalculatedTotalRate(ReservationBookingRecord booking, long stayedNights, long originalNights) {
        long unusedNights = originalNights - stayedNights;
        BigDecimal baseRate = booking.getRate()
                .multiply(BigDecimal.valueOf(booking.getNumberOfRooms().longValue()));

        if (unusedNights > 0) {
            BigDecimal penalty = baseRate;
            BigDecimal stayedAmount = baseRate.multiply(BigDecimal.valueOf(stayedNights));
            return stayedAmount.add(penalty).setScale(2, RoundingMode.HALF_UP);
        }

        return baseRate.multiply(BigDecimal.valueOf(stayedNights)).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateBookingCharges(ReservationBookingRecord booking, long originalNights) {
        if (booking.getRate() == null || booking.getNumberOfRooms() == null
                || booking.getArrivalDate() == null || booking.getDepartureDate() == null) {
            return;
        }

        long stayedNights = ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate());
        if (stayedNights < 0) {
            booking.setTotalRate(BigDecimal.ZERO);
            return;
        }

        booking.setTotalRate(computeRecalculatedTotalRate(booking, stayedNights, originalNights));
    }

    private BigDecimal calculateRecalculatedRate(ReservationBookingRecord booking, LocalDate newDepartureDate, long originalNights) {
        if (booking.getRate() == null || booking.getNumberOfRooms() == null
                || booking.getArrivalDate() == null || newDepartureDate == null) {
            return booking.getTotalRate() == null ? BigDecimal.ZERO : booking.getTotalRate();
        }

        long stayedNights = ChronoUnit.DAYS.between(booking.getArrivalDate(), newDepartureDate);
        if (stayedNights < 0) {
            return BigDecimal.ZERO;
        }

        return computeRecalculatedTotalRate(booking, stayedNights, originalNights);
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
                .changedFields("reservationStatus, roomOccupancy, departureDate, totalRate")
                .actor(StringUtils.hasText(actor) ? actor.trim() : "system")
                .build());
    }

    private CheckoutCompletionResponseDto toResponse(ReservationBookingRecord booking, LocalDate earlyDepartureDate, BigDecimal refundAmount) {
        return CheckoutCompletionResponseDto.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .reservationStatus(booking.getReservationStatus())
                .businessDate(booking.getCheckOutBusinessDate())
                .checkOutCompletedAt(booking.getCheckOutCompletedAt())
                .checkOutCompletedBy(booking.getCheckOutCompletedBy())
                .earlyDepartureDate(earlyDepartureDate)
                .recalculatedTotalRate(booking.getTotalRate())
                .updatedGuestBalance(booking.getGuestBalance())
                .refundAmount(refundAmount)
                .message(earlyDepartureDate != null ? "Early check-out applied; charges recalculated" : null)
                .build();
    }
}
