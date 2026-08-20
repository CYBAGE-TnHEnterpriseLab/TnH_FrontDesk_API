package com.pms.inventory.availability.service;

import com.pms.inventory.availability.dto.AvailabilityResponse;
import com.pms.inventory.inventory.entity.RoomTypeInventoryDaily;
import com.pms.inventory.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final InventoryService inventoryService;

    public AvailabilityService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(
            UUID propertyId,
            UUID roomTypeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<RoomTypeInventoryDaily> rows = inventoryService.getInventoryRange(propertyId, roomTypeId, fromDate, toDate);
        return rows.stream()
                .map(row -> new AvailabilityResponse(
                        row.getPropertyId(),
                        row.getRoomTypeId(),
                        row.getBusinessDate(),
                        row.getTotalInventory(),
                        row.getReservedCount(),
                        row.getBlockedCount(),
                        inventoryService.availableCount(row)
                ))
                .toList();
    }
}

