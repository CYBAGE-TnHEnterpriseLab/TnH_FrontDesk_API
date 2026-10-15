package com.pms.inventory.inventory.dto.response;

import java.time.LocalDate;

public record DailyInventoryResponse(
		String propertyId,
		String roomTypeId,
		LocalDate businessDate,
		Integer totalInventory,
		Integer reservedCount,
		Integer blockedCount,
		Integer availableCount
) {
}

