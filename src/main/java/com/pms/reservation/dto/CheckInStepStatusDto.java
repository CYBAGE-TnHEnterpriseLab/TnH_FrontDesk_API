package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInStepStatusDto {
    String code;
    String label;
    int sequence;
    boolean mandatory;
    boolean completed;
    LocalDateTime completedAt;
}
