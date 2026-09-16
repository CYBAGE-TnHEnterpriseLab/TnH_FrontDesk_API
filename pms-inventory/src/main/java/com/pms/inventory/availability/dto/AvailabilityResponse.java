package com.pms.inventory.availability.dto;

import java.time.LocalDate;
public record AvailabilityResponse(
        String propertyId,
        String roomTypeId,
        LocalDate businessDate,
        Integer totalInventory,
        Integer reservedCount,
        Integer blockedCount,
        Integer availableCount
) {
}

