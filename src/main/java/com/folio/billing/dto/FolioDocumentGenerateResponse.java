package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FolioDocumentGenerateResponse(
        String documentId,
        String confirmationNo,
        FolioDocumentType documentType,
        String fileName,
        String contentType,
        LocalDateTime generatedAt,
        String generatedBy,
        BigDecimal totalChargeAmount,
        BigDecimal totalTaxAmount,
        BigDecimal totalPaymentAmount,
        BigDecimal latestBalance,
        String downloadPath,
        String printPath
) {
}
