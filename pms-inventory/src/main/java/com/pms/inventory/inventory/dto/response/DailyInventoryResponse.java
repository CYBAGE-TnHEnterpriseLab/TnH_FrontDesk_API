package com.pms.inventory.inventory.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record DailyInventoryResponse(
		UUID propertyId,
		UUID roomTypeId,
		LocalDate businessDate,
		Integer totalInventory,
		Integer reservedCount,
		Integer blockedCount,
		Integer availableCount
) {
}

