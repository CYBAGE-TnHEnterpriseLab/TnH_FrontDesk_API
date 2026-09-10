package com.pms.inventory.reservation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
public record ReserveInventoryRequest(
        @NotNull(message = "reservationId is required")
        String confirmationNumber,
        @NotNull(message = "propertyId is required")
        String propertyId,
        @NotNull(message = "bookedRoomTypeId is required")
        String bookedRoomTypeId,
        @NotNull(message = "assignedRoomTypeId is required")
        String assignedRoomTypeId,
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

