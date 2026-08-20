package com.pms.inventory.inventory.controller;

import com.pms.inventory.common.response.ApiResponse;
import com.pms.inventory.inventory.dto.response.DailyInventoryResponse;
import com.pms.inventory.inventory.dto.response.PropertyDeletionCheckResponse;
import com.pms.inventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/daily")
    @Operation(summary = "Get one daily inventory row")
    public DailyInventoryResponse getDailyInventory(
            @RequestParam @NotNull UUID propertyId,
            @RequestParam @NotNull UUID roomTypeId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) {
        return inventoryService.getDailyInventory(propertyId, roomTypeId, businessDate);
    }

    @GetMapping("/properties/{propertyId}/deletion-check")
    public ResponseEntity<ApiResponse<PropertyDeletionCheckResponse>> checkPropertyDeletion(
            @PathVariable UUID propertyId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate
    ) {
        PropertyDeletionCheckResponse response =
                inventoryService.hasAnyActiveReservations(
                        propertyId,
                        businessDate,
                        0
                );

        return ResponseEntity.ok(
                ApiResponse.ok(response, "Property deletion check completed")
        );
    }
}

