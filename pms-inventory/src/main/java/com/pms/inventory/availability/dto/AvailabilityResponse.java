package com.pms.inventory.availability.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityResponse(
        UUID propertyId,
        UUID roomTypeId,
        LocalDate businessDate,
        Integer totalInventory,
        Integer reservedCount,
        Integer blockedCount,
        Integer availableCount
) {
}

