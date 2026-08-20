package com.pms.inventory.block.dto.response;

import com.pms.inventory.block.enums.InventoryBlockStatus;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryBlockResponse(
        Long blockId,
        UUID propertyId,
        UUID roomTypeId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer quantity,
        String reason,
        InventoryBlockStatus status,
        boolean idempotent
) {
}

