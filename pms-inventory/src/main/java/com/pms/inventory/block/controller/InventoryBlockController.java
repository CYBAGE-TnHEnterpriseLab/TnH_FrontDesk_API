package com.pms.inventory.block.controller;

import com.pms.inventory.block.dto.request.CreateInventoryBlockRequest;
import com.pms.inventory.block.dto.response.InventoryBlockResponse;
import com.pms.inventory.block.service.InventoryBlockService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/blocks")
public class InventoryBlockController {

    private final InventoryBlockService inventoryBlockService;

    public InventoryBlockController(InventoryBlockService inventoryBlockService) {
        this.inventoryBlockService = inventoryBlockService;
    }

    @PostMapping
    @Operation(summary = "Create inventory block")
    public InventoryBlockResponse create(@Valid @RequestBody CreateInventoryBlockRequest request) {
        return inventoryBlockService.create(request);
    }

    @PostMapping("/{blockId}/release")
    @Operation(summary = "Release inventory block")
    public InventoryBlockResponse release(@PathVariable Long blockId) {
        return inventoryBlockService.release(blockId);
    }
}

