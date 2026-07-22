package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInAuditEventDto {
    String eventType;
    String eventMessage;
    String changedFields;
    String actor;
    LocalDateTime createdAt;
}
