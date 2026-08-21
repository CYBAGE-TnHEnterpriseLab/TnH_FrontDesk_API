package com.folio.billing.dto;

import java.math.BigDecimal;

public record FolioPaymentAllocationLineResult(
        String confirmationNumber,
        String transactionReferenceNumber,
        BigDecimal allocatedAmount,
        BigDecimal balanceBeforeAllocation,
        BigDecimal balanceAfterAllocation
) {
}

