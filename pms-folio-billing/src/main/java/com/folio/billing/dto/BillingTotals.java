package com.folio.billing.dto;

import java.math.BigDecimal;

public record BillingTotals(
        BigDecimal totalCharges,
        BigDecimal totalPayment
) {
}
