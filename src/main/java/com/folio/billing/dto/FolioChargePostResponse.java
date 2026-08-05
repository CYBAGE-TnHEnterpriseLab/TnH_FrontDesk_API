package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolioChargePostResponse(
        String confirmationNo,
        String referenceNumber,
        String transactionType,
        String category,
        String description,
        BigDecimal amount,
        BigDecimal tax,
        LocalDate postingDate,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance
) {
}
