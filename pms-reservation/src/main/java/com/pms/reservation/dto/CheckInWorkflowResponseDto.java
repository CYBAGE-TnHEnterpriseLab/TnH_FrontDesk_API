package com.pms.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInWorkflowResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    String reservationStatus;
    String currentStep;
    int completedSteps;
    int totalSteps;
    int progressPercent;
    boolean canCompleteCheckIn;
    LocalDateTime checkInCompletedAt;
    String checkInCompletedBy;
    List<CheckInStepStatusDto> steps;
    GuestContactDetailsDto guestContactDetails;
    RoomStayDetailsDto roomStayDetails;
    SignatureSummaryDto signature;
}
