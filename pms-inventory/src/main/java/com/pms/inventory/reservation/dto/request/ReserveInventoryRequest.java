package com.pms.inventory.reservation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull(message = "reservationId is required")
        UUID reservationId,
        @NotNull(message = "propertyId is required")
        UUID propertyId,
        @NotNull(message = "bookedRoomTypeId is required")
        UUID bookedRoomTypeId,
        @NotNull(message = "assignedRoomTypeId is required")
        UUID assignedRoomTypeId,
        @NotNull(message = "checkInDate is required")
        LocalDate checkInDate,
        @NotNull(message = "checkOutDate is required")
        LocalDate checkOutDate,
        @Positive(message = "quantity must be greater than 0")
        Integer quantity
) {
    @AssertTrue(message = "checkOutDate must be after checkInDate")
    public boolean isDateRangeValid() {
        return checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate);
    }
}

