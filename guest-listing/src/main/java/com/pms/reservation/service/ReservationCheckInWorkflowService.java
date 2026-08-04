package com.pms.reservation.service;

import com.pms.reservation.dto.CheckInAuditHistoryResponseDto;
import com.pms.reservation.dto.CheckInAuditPageResponseDto;
import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;
import com.pms.reservation.dto.CheckInGuestUpdateRequestDto;
import com.pms.reservation.dto.CheckInPaymentValidationResponseDto;
import com.pms.reservation.dto.CheckInRoomStayUpdateRequestDto;
import com.pms.reservation.dto.CheckInSignatureRequestDto;
import com.pms.reservation.dto.CheckInSignatureResponseDto;
import com.pms.reservation.dto.CheckInStepProgressResponseDto;
import com.pms.reservation.dto.CheckInWorkflowResponseDto;

public interface ReservationCheckInWorkflowService {

    CheckInWorkflowResponseDto getWorkflow(Long bookingId);

    CheckInWorkflowResponseDto updateGuestDetails(Long bookingId, CheckInGuestUpdateRequestDto request, String actor);

    CheckInWorkflowResponseDto updateRoomStay(Long bookingId, CheckInRoomStayUpdateRequestDto request, String actor);

    CheckInWorkflowResponseDto saveSignature(Long bookingId, CheckInSignatureRequestDto request, String actor);

    CheckInPaymentValidationResponseDto validatePayment(Long bookingId, String actor);

    CheckInCompletionResponseDto completeCheckIn(Long bookingId, CheckInCompleteRequestDto request);

    CheckInSignatureResponseDto getSignature(Long bookingId);

    CheckInStepProgressResponseDto getStepProgress(Long bookingId);

    CheckInAuditHistoryResponseDto getAuditHistory(Long bookingId);

        CheckInAuditPageResponseDto getAuditHistoryPage(
            Long bookingId,
            String eventType,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            int page,
            int size,
            String sortDir
        );
}
