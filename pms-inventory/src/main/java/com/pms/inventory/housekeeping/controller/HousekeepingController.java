package com.pms.inventory.housekeeping.controller;

import com.pms.inventory.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.inventory.housekeeping.dto.response.AssignableRoomResponse;
import com.pms.inventory.housekeeping.dto.response.HousekeepingDashboardResponse;
import com.pms.inventory.housekeeping.dto.response.HousekeepingRoomsPageResponse;
import com.pms.inventory.housekeeping.dto.response.HousekeepingStatusUpdateResponse;
import com.pms.inventory.housekeeping.entity.CleaningStatus;
import com.pms.inventory.housekeeping.entity.FrontOfficeStatus;
import com.pms.inventory.housekeeping.entity.ReservationStatus;
import com.pms.inventory.housekeeping.service.HousekeepingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/housekeeping")
@Validated
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    public HousekeepingController(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get housekeeping dashboard counters")
    public HousekeepingDashboardResponse dashboard(
            @RequestParam @NotNull UUID propertyId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate
    ) {
        return housekeepingService.dashboard(propertyId, businessDate);
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get housekeeping rooms with filters")
    public HousekeepingRoomsPageResponse rooms(
            @RequestParam @NotNull UUID propertyId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(required = false) List<CleaningStatus> cleaningStatus,
            @RequestParam(required = false) List<FrontOfficeStatus> frontOfficeStatus,
            @RequestParam(required = false) List<ReservationStatus> reservationStatus,
            @RequestParam(required = false) UUID roomTypeId,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String roomClass,
            @RequestParam(required = false) String attendant,
            @RequestParam(required = false) Boolean vipOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "roomNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return housekeepingService.rooms(
                propertyId,
                businessDate,
                cleaningStatus,
                frontOfficeStatus,
                reservationStatus,
                roomTypeId,
                floor,
                zone,
                roomClass,
                attendant,
                vipOnly,
                page,
                size,
                sortBy,
                sortDir
        );
    }

    @GetMapping("/assignable-rooms")
    @Operation(summary = "Get assignable room numbers for dropdown")
    public List<AssignableRoomResponse> assignableRooms(
            @RequestParam @NotNull UUID propertyId,
            @RequestParam @NotNull UUID roomTypeId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return housekeepingService.assignableRooms(propertyId, businessDate, roomTypeId, limit);
    }

    @PatchMapping("/rooms/{roomNumber}/status")
    @Operation(summary = "Update housekeeping status for a room")
    public HousekeepingStatusUpdateResponse updateRoomStatus(
            @PathVariable String roomNumber,
            @Valid @RequestBody UpdateHousekeepingStatusRequest request
    ) {
        return housekeepingService.updateRoomStatus(roomNumber, request);
    }
}

