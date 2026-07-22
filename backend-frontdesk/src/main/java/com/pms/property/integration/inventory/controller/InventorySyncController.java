package com.pms.property.integration.inventory.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.integration.inventory.dto.InventorySyncStatusResponse;
import com.pms.property.integration.inventory.service.InventorySyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/inventory-sync")
public class InventorySyncController {

    private final InventorySyncService inventorySyncService;

    public InventorySyncController(InventorySyncService inventorySyncService) {
        this.inventorySyncService = inventorySyncService;
    }

    @PostMapping("/{propertyId}/retry")
    public ApiResponse<InventorySyncStatusResponse> retry(@PathVariable String propertyId) {
        return ApiResponse.ok(inventorySyncService.syncNow(propertyId), "Inventory sync completed");
    }

    @GetMapping("/{propertyId}/status")
    public ApiResponse<InventorySyncStatusResponse> status(@PathVariable String propertyId) {
        return ApiResponse.ok(inventorySyncService.getStatus(propertyId), "Inventory sync status fetched");
    }
}


