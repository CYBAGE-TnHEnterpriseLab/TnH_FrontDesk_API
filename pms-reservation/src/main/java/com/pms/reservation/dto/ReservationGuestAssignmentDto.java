package com.pms.reservation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationGuestAssignmentDto {
    Long bookingId;
    String confirmationNumber;
    Long guestProfileId;
    Boolean isPrimary;
}
