package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FolioTransactionRow(
        LocalDate date,
        String referenceNumber,
        String transactionType,
        String category,
        String description,
        BigDecimal charges,
        BigDecimal tax,
        BigDecimal credit,
        String userId,
        LocalDateTime postedAt,
        String originalReferenceNumber,
        String adjustmentReason,
        List<TaxDetail> taxDetails
) {
    public BigDecimal totalAmount() {
        return (charges == null ? BigDecimal.ZERO : charges).add(tax == null ? BigDecimal.ZERO : tax);
    }
}
