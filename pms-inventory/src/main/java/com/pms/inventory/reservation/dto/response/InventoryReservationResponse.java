package com.pms.inventory.reservation.dto.response;

import com.pms.inventory.reservation.enums.InventoryReservationStatus;

import java.time.LocalDate;
public record InventoryReservationResponse(
        String confirmationNumber,
        String propertyId,
        String bookedRoomTypeId,
        String assignedRoomTypeId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer quantity,
        InventoryReservationStatus status,
        boolean idempotent
) {
}

