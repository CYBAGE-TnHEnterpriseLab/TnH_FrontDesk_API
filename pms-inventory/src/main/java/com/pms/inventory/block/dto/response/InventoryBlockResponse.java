package com.pms.inventory.block.dto.response;

import com.pms.inventory.block.enums.InventoryBlockStatus;

import java.time.LocalDate;

public record InventoryBlockResponse(
        Long blockId,
        String propertyId,
        String roomTypeId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer quantity,
        String reason,
        InventoryBlockStatus status,
        boolean idempotent
) {
}

