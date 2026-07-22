package com.pms.housekeeping.controller;

import com.pms.housekeeping.dto.request.RoomMasterSyncRequest;
import com.pms.housekeeping.service.RoomMasterSyncService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/housekeeping/room-master")
public class RoomMasterSyncController {

    private final RoomMasterSyncService roomMasterSyncService;

    public RoomMasterSyncController(RoomMasterSyncService roomMasterSyncService) {
        this.roomMasterSyncService = roomMasterSyncService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync room master projection and initialize housekeeping room day rows")
    public ResponseEntity<Map<String, Integer>> sync(@Valid @RequestBody RoomMasterSyncRequest request) {
        var result = roomMasterSyncService.sync(request);
        return ResponseEntity.ok(Map.of(
                "syncedRooms", result.syncedRooms(),
                "deactivatedRooms", result.deactivatedRooms()
        ));
    }
}



