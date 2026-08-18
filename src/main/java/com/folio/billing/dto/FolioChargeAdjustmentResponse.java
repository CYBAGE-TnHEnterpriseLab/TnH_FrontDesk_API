package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FolioChargeAdjustmentResponse(
        String confirmationNo,
        String originalReferenceNumber,
        String adjustmentReferenceNumber,
        ChargeAdjustmentType adjustmentType,
        String category,
        String reason,
        BigDecimal amount,
        BigDecimal tax,
        List<TaxDetail> taxDetails,
        BigDecimal totalAmount,
        LocalDate postingDate,
        LocalDateTime postedAt,
        String userId,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance
) {
}
