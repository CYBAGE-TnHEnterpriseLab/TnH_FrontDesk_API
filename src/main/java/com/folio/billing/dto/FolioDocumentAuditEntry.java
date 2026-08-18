package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FolioDocumentAuditEntry(
        String documentId,
        String confirmationNo,
        FolioDocumentType documentType,
        String fileName,
        LocalDateTime generatedAt,
        String generatedBy,
        BigDecimal totalChargeAmount,
        BigDecimal totalTaxAmount,
        BigDecimal totalPaymentAmount,
        BigDecimal latestBalance
) {
}
