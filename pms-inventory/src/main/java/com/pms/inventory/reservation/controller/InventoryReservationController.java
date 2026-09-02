package com.pms.inventory.reservation.controller;

import com.pms.inventory.reservation.dto.request.ChangeAssignedRoomTypeRequest;
import com.pms.inventory.reservation.dto.request.ReserveInventoryRequest;
import com.pms.inventory.reservation.dto.response.InventoryReservationResponse;
import com.pms.inventory.reservation.service.InventoryReservationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/inventory/reservations")
public class InventoryReservationController {

    private final InventoryReservationService inventoryReservationService;

    public InventoryReservationController(InventoryReservationService inventoryReservationService) {
        this.inventoryReservationService = inventoryReservationService;
    }

    @PostMapping
    @Operation(summary = "Reserve room-type inventory")
    public InventoryReservationResponse reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        return inventoryReservationService.reserve(request);
    }

    @PostMapping("/{confirmationNumber}/release")
    @Operation(summary = "Release reserved room-type inventory")
    public InventoryReservationResponse release(@PathVariable String confirmationNumber) {
        return inventoryReservationService.release(confirmationNumber);
    }

    @PutMapping("/{confirmationNumber}/assigned-room-type")
    @Operation(summary = "Change assigned room type for inventory consumption")
    public InventoryReservationResponse changeAssignedRoomType(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody ChangeAssignedRoomTypeRequest request
    ) {
        return inventoryReservationService.changeAssignedRoomType(confirmationNumber, request);
    }
}

