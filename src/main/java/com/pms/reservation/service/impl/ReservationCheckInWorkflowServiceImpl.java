package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationCheckInAuditRepository;
import com.pms.reservation.service.ReservationCheckInWorkflowService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationCheckInWorkflowServiceImpl implements ReservationCheckInWorkflowService {

    private static final String STATUS_ARRIVED = "ARRIVED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationCheckInAuditRepository auditRepository;
    private final HousekeepingRoomStatusService housekeepingRoomStatusService;

    /**
     * Completes check-in by confirmation number. Payment validation and signature capture
     * are deliberately not prerequisites for this operation.
     */
    @Override
    @Transactional
    public CheckInCompletionResponseDto completeCheckIn(String confirmationNumber, CheckInCompleteRequestDto request) {
        ReservationBookingRecord booking = reservationBookingRepository.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BadRequestException("Reservation booking not found"));
        String targetStatus = resolveTargetStatus(request.getTargetStatus());

        if (isAlreadyCompleted(booking)) {
            if (STATUS_CHECKED_IN.equals(targetStatus) && STATUS_ARRIVED.equals(booking.getReservationStatus())) {
                booking.setReservationStatus(STATUS_CHECKED_IN);
                booking.setCheckInCompletedAt(LocalDateTime.now());
                booking.setCheckInCompletedBy(request.getActor().trim());
                booking.setCheckInBusinessDate(request.getBusinessDate());
                reservationBookingRepository.save(booking);
                appendAudit(booking, "CHECKIN_STATUS_UPGRADED", "Check-in status upgraded from ARRIVED to CHECKED_IN",
                        "reservationStatus", request.getActor());
            } else if (!targetStatus.equals(booking.getReservationStatus())) {
                throw new BadRequestException("Check-in already completed with status " + booking.getReservationStatus());
            }
            return toResponse(booking);
        }

        booking.setReservationStatus(targetStatus);
        booking.setCheckInCompletedAt(LocalDateTime.now());
        booking.setCheckInCompletedBy(request.getActor().trim());
        booking.setCheckInBusinessDate(request.getBusinessDate());
        reservationBookingRepository.save(booking);

        HousekeepingRoomStatusRequestDto housekeepingRequest = new HousekeepingRoomStatusRequestDto();
        housekeepingRequest.setPropertyId(booking.getPropertyId());
        housekeepingRequest.setBusinessDate(request.getBusinessDate());
        housekeepingRequest.setConfirmationNumber(booking.getConfirmationNumber());
        housekeepingRequest.setRoomNo(booking.getAssignedRoomNo());
        housekeepingRoomStatusService.markOccupied(housekeepingRequest);

        appendAudit(booking, "CHECKIN_COMPLETED", "Check-in completed successfully",
                "reservationStatus, roomOccupancy", request.getActor());
        return toResponse(booking);
    }

    private boolean isAlreadyCompleted(ReservationBookingRecord booking) {
        return booking.getCheckInCompletedAt() != null
                || STATUS_ARRIVED.equals(booking.getReservationStatus())
                || STATUS_CHECKED_IN.equals(booking.getReservationStatus());
    }

    private String resolveTargetStatus(String targetStatus) {
        if (!StringUtils.hasText(targetStatus)) {
            return STATUS_CHECKED_IN;
        }
        String normalized = targetStatus.trim().toUpperCase();
        if (!STATUS_ARRIVED.equals(normalized) && !STATUS_CHECKED_IN.equals(normalized)) {
            throw new BadRequestException("targetStatus must be ARRIVED or CHECKED_IN");
        }
        return normalized;
    }

    private void appendAudit(ReservationBookingRecord booking, String eventType, String message, String changedFields, String actor) {
        auditRepository.save(ReservationCheckInAuditRecord.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getConfirmationNumber())
                .propertyId(booking.getPropertyId())
                .eventType(eventType)
                .eventMessage(message)
                .changedFields(changedFields)
                .actor(StringUtils.hasText(actor) ? actor.trim() : "system")
                .createdAt(LocalDateTime.now())
                .build());
    }

    private CheckInCompletionResponseDto toResponse(ReservationBookingRecord booking) {
        return CheckInCompletionResponseDto.builder()
                .confirmationNumber(booking.getConfirmationNumber())
                .reservationStatus(booking.getReservationStatus())
                .checkInCompletedAt(booking.getCheckInCompletedAt())
                .checkInCompletedBy(booking.getCheckInCompletedBy())
                .build();
    }
}
