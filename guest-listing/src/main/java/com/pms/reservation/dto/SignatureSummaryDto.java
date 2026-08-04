package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SignatureSummaryDto {
    boolean present;
    String contentType;
    LocalDateTime signedAt;
}
