package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentAllocationHistoryEntry(
        String paymentReference,
        String confirmationNo,
        BigDecimal paymentAmount,
        BigDecimal totalAllocatedAmount,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        String paymentMethod,
        LocalDate allocationDate,
        LocalDateTime allocatedAt,
        String userId,
        String note,
        BigDecimal balanceAfterAllocation
) {
}
