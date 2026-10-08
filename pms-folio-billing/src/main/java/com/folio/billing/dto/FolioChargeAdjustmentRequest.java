package com.folio.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FolioChargeAdjustmentRequest(
        @NotBlank(message = "confirmationNumber is required")
        String confirmationNumber,
        @NotBlank(message = "originalReferenceNumber is required")
        String originalReferenceNumber,
        @NotNull(message = "adjustmentType is required")
        ChargeAdjustmentType adjustmentType,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,
        @NotBlank(message = "reason is required")
        String reason,
                String userId,
                Long bookingId
) {
        public FolioChargeAdjustmentRequest(String confirmationNumber, String originalReferenceNumber,
                                                                                ChargeAdjustmentType adjustmentType, BigDecimal amount,
                                                                                String reason, String userId) {
                this(confirmationNumber, originalReferenceNumber, adjustmentType, amount, reason, userId, null);
        }
}

