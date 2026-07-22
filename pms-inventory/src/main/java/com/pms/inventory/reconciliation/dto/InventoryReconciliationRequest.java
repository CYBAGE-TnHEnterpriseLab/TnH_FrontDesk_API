package com.pms.inventory.reconciliation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReconciliationRequest(
        @NotNull(message = "propertyId is required")
        UUID propertyId,
        @NotNull(message = "fromDate is required")
        LocalDate fromDate,
        @NotNull(message = "toDate is required")
        LocalDate toDate,
        @Valid
        @NotEmpty(message = "roomTypes is required")
        List<RoomTypeInventoryInput> roomTypes
) {
    @AssertTrue(message = "toDate must be after fromDate")
    public boolean isDateRangeValid() {
        return fromDate != null && toDate != null && toDate.isAfter(fromDate);
    }

    public record RoomTypeInventoryInput(
            @NotNull(message = "roomTypeId is required")
            UUID roomTypeId,
            @NotNull(message = "totalInventory is required")
            @PositiveOrZero(message = "totalInventory must be >= 0")
            Integer totalInventory
    ) {
    }
}

