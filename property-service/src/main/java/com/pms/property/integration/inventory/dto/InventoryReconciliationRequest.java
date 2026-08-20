package com.pms.property.integration.inventory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InventoryReconciliationRequest(
    UUID propertyId,
    LocalDate fromDate,
    LocalDate toDate,
    List<RoomTypeInventoryInput> roomTypes
) {

    public record RoomTypeInventoryInput(
        UUID roomTypeId,
        Integer totalInventory
    ) {
    }
}

