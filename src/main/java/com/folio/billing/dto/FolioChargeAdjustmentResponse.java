package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FolioChargeAdjustmentResponse(
        String confirmationNo,
        String originalReferenceNumber,
        String adjustmentReferenceNumber,
        ChargeAdjustmentType adjustmentType,
        String category,
        String reason,
        BigDecimal amount,
        BigDecimal tax,
        LocalDate postingDate,
        LocalDateTime postedAt,
        String userId,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance
) {
}
