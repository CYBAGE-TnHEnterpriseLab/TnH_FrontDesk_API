package com.pms.inventory.reservation.dto.response;

import com.pms.inventory.reservation.enums.InventoryReservationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID reservationId,
        UUID propertyId,
        UUID bookedRoomTypeId,
        UUID assignedRoomTypeId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer quantity,
        InventoryReservationStatus status,
        boolean idempotent
) {
}

