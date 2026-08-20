package com.pms.property.integration.inventory.dto;

public record InventorySyncStatusResponse(
    String propertyId,
    String status,
    String lastRequestId,
    String lastError,
    Integer retryCount,
    String lastSyncedAt,
    String updatedAt
) {
}

