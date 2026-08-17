package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInCompletionResponseDto {
    String confirmationNumber;
    String reservationStatus;
    LocalDateTime checkInCompletedAt;
    String checkInCompletedBy;
}
