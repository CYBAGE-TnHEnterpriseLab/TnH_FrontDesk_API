package com.pms.reservation.integration.dto;

import java.time.LocalDate;
import lombok.Value;

@Value
public class InventoryAvailabilityDto {
    String propertyId;
    String roomTypeId;
    LocalDate businessDate;
    Integer totalInventory;
    Integer reservedCount;
    Integer blockedCount;
    Integer availableCount;
}
