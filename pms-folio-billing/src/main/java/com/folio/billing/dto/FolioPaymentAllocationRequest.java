package com.folio.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FolioPaymentAllocationRequest(
        @NotNull(message = "paymentAmount is required")
        @DecimalMin(value = "0.01", message = "paymentAmount must be greater than zero")
        BigDecimal paymentAmount,
        String paymentReference,
        String paymentMethod,
        LocalDate allocationDate,
        @NotEmpty(message = "allocations are required")
        List<@Valid PaymentAllocationTargetRequest> allocations,
        String userId,
        String note
) {
}
