package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentProcessingResult {
    String status;
    String transactionReference;
    String processorName;
    String failureReason;
    LocalDateTime processedAt;
}
