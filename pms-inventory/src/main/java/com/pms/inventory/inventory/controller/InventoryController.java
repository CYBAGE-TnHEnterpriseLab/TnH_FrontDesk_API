package com.pms.inventory.inventory.controller;

import com.pms.inventory.inventory.dto.response.DailyInventoryResponse;
import com.pms.inventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

