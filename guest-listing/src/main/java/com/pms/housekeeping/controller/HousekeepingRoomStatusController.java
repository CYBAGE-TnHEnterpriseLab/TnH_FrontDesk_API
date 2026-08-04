package com.pms.housekeeping.controller;

import com.pms.guestlisting.dto.ApiResponse;
import com.pms.housekeeping.dto.HousekeepingManualStatusUpdateRequestDto;
import com.pms.housekeeping.dto.HousekeepingRoomStatusRequestDto;
import com.pms.housekeeping.dto.HousekeepingRoomStatusResponseDto;
import com.pms.housekeeping.service.HousekeepingRoomStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/housekeeping/rooms")
@RequiredArgsConstructor
@Tag(name = "Housekeeping Room Status", description = "APIs to manage room status lifecycle")
public class HousekeepingRoomStatusController {

    private final HousekeepingRoomStatusService housekeepingRoomStatusService;

    @PostMapping("/check-in")
    @Operation(summary = "Mark room as occupied for check-in")
    public ResponseEntity<ApiResponse<HousekeepingRoomStatusResponseDto>> markOccupied(
            @Valid @RequestBody HousekeepingRoomStatusRequestDto request
    ) {
        HousekeepingRoomStatusResponseDto response = housekeepingRoomStatusService.markOccupied(request);
        return ResponseEntity.ok(ApiResponse.success("Room status updated to OCCUPIED", response));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Mark room as dirty for check-out")
    public ResponseEntity<ApiResponse<HousekeepingRoomStatusResponseDto>> markDirty(
            @Valid @RequestBody HousekeepingRoomStatusRequestDto request
    ) {
        HousekeepingRoomStatusResponseDto response = housekeepingRoomStatusService.markDirty(request);
        return ResponseEntity.ok(ApiResponse.success("Room status updated to DIRTY", response));
    }

    @PatchMapping("/status")
    @Operation(summary = "Manually change room status, e.g. DIRTY to CLEANED")
    public ResponseEntity<ApiResponse<HousekeepingRoomStatusResponseDto>> updateStatus(
            @Valid @RequestBody HousekeepingManualStatusUpdateRequestDto request
    ) {
        HousekeepingRoomStatusResponseDto response = housekeepingRoomStatusService.updateManualStatus(
                request,
                request.getRoomStatus()
        );
        return ResponseEntity.ok(ApiResponse.success("Room status updated successfully", response));
    }
}
