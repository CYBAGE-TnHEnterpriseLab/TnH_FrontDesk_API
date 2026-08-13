package com.folio.billing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolioChargePostRequest(
        @NotBlank(message = "confirmationNo is required")
        String confirmationNo,
        String roomNo,
        String guestName,
        @NotBlank(message = "category is required")
        String category,
        @NotBlank(message = "description is required")
        String description,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate postingDate,
        String userId
) {
}
