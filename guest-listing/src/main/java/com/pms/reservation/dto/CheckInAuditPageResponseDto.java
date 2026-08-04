package com.pms.reservation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInAuditPageResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    CheckInAuditFilterRequestDto filters;
    List<CheckInAuditEventDto> events;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
}
