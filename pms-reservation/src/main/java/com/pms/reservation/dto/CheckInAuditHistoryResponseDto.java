package com.pms.reservation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInAuditHistoryResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    int totalEvents;
    List<CheckInAuditEventDto> events;
}
