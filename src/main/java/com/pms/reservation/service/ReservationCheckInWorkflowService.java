package com.pms.reservation.service;

import com.pms.reservation.dto.CheckInCompleteRequestDto;
import com.pms.reservation.dto.CheckInCompletionResponseDto;

public interface ReservationCheckInWorkflowService {

    CheckInCompletionResponseDto completeCheckIn(String confirmationNumber, CheckInCompleteRequestDto request);
}
