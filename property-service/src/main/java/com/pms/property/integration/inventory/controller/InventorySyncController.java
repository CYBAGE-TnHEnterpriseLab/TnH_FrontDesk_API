package com.pms.property.integration.inventory.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.integration.inventory.dto.InventorySyncStatusResponse;
import com.pms.property.integration.inventory.service.InventorySyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/inventory-sync")
public class InventorySyncController {

    private final InventorySyncService inventorySyncService;
    private final HttpServletRequest request;

    public InventorySyncController(InventorySyncService inventorySyncService, HttpServletRequest request) {
        this.inventorySyncService = inventorySyncService;
        this.request = request;
    }

    @PostMapping("/{propertyId}/retry")
    public ApiResponse<InventorySyncStatusResponse> retry(@PathVariable String propertyId) {
        String authHeader = request.getHeader("Authorization");
        return ApiResponse.ok(inventorySyncService.syncNow(propertyId, authHeader), "Inventory sync completed");
    }

    @GetMapping("/{propertyId}/status")
    public ApiResponse<InventorySyncStatusResponse> status(@PathVariable String propertyId) {
        return ApiResponse.ok(inventorySyncService.getStatus(propertyId), "Inventory sync status fetched");
    }
}
