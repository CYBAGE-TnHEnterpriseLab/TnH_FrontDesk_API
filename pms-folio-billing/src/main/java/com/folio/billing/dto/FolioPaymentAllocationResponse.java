package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FolioPaymentAllocationResponse(
        String paymentReference,
        BigDecimal paymentAmount,
        BigDecimal totalAllocatedAmount,
        BigDecimal unallocatedAmount,
        String paymentMethod,
        LocalDate allocationDate,
        LocalDateTime allocatedAt,
        String userId,
        List<FolioPaymentAllocationLineResult> allocations
) {
}
