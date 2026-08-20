package com.pms.housekeeping.controller;

import com.pms.housekeeping.dto.request.HousekeepingRoomFilterRequest;
import com.pms.housekeeping.dto.request.UpdateHousekeepingStatusRequest;
import com.pms.housekeeping.dto.response.*;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.ReservationStatus;
import com.pms.housekeeping.service.HousekeepingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/housekeeping")
@Tag(name = "Housekeeping", description = "Housekeeping Management APIs")
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
    @Operation(summary = "Get housekeeping rooms")
    public HousekeepingRoomsPageResponse rooms(@Valid @ModelAttribute HousekeepingRoomFilterRequest request
    ) {
        return housekeepingService.rooms(request);
    }

    @GetMapping("/rooms/calendar")
    @Operation(summary = "Get housekeeping rooms for calendar view")
    public HousekeepingCalendarResponse calendar(
            @RequestParam @NotNull UUID propertyId,

            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            UUID roomTypeId
    ) {
        return housekeepingService.calendar(
                propertyId,
                fromDate,
                toDate,
                roomTypeId
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
