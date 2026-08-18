package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FolioChargePostResponse(
        String confirmationNo,
        String referenceNumber,
        String transactionType,
        String category,
        String description,
        BigDecimal amount,
        BigDecimal tax,
        List<TaxDetail> taxDetails,
        BigDecimal totalAmount,
        LocalDate postingDate,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance
) {
}
