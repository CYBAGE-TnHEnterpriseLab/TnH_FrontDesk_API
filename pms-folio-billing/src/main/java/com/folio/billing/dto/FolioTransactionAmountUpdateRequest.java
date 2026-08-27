package com.folio.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FolioTransactionAmountUpdateRequest(
        @NotBlank String confirmationNumber,
        @NotBlank String referenceNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String userId
) {}
