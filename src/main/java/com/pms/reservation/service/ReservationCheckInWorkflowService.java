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

    CheckInWorkflowResponseDto getWorkflow(String confirmationNumber);

    CheckInWorkflowResponseDto updateGuestDetails(String confirmationNumber, CheckInGuestUpdateRequestDto request, String actor);

    CheckInWorkflowResponseDto updateRoomStay(String confirmationNumber, CheckInRoomStayUpdateRequestDto request, String actor);

    CheckInWorkflowResponseDto saveSignature(String confirmationNumber, CheckInSignatureRequestDto request, String actor);

    CheckInPaymentValidationResponseDto validatePayment(String confirmationNumber, String actor);

    CheckInCompletionResponseDto completeCheckIn(String confirmationNumber, CheckInCompleteRequestDto request);

    CheckInSignatureResponseDto getSignature(String confirmationNumber);

    CheckInStepProgressResponseDto getStepProgress(String confirmationNumber);

    CheckInAuditHistoryResponseDto getAuditHistory(String confirmationNumber);

        CheckInAuditPageResponseDto getAuditHistoryPage(
            String confirmationNumber,
            String eventType,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            int page,
            int size,
            String sortDir
        );
}
