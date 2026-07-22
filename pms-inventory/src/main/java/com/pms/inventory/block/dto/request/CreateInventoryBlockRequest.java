package com.pms.inventory.block.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record CreateInventoryBlockRequest(
        @NotNull(message = "propertyId is required")
        UUID propertyId,
        @NotNull(message = "roomTypeId is required")
        UUID roomTypeId,
        @NotNull(message = "fromDate is required")
        LocalDate fromDate,
        @NotNull(message = "toDate is required")
        LocalDate toDate,
        @Positive(message = "quantity must be greater than 0")
        Integer quantity,
        @NotBlank(message = "reason is required")
        String reason
) {
    @AssertTrue(message = "toDate must be after fromDate")
    public boolean isDateRangeValid() {
        return fromDate != null && toDate != null && toDate.isAfter(fromDate);
    }
}

