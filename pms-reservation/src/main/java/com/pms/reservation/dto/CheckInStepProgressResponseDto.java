package com.pms.reservation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInStepProgressResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    String currentStep;
    int completedSteps;
    int totalSteps;
    int progressPercent;
    boolean canCompleteCheckIn;
    List<CheckInStepStatusDto> steps;
}
