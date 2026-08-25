package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FolioTransactionRow(
        LocalDate date,
        String referenceNumber,
        String transactionType,
        String category,
        String description,
        BigDecimal charges,
        BigDecimal credit,
        String userId,
        LocalDateTime postedAt,
        String originalReferenceNumber,
        String adjustmentReason
) {
    public BigDecimal totalAmount() {
        return charges == null ? BigDecimal.ZERO : charges;
    }
}
