package com.pms.reservation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationRoomBookingSummaryDto {
    Long bookingId;
    String confirmationNumber;
    String guestName;
    String assignedRoomNo;
    Integer floor;
}