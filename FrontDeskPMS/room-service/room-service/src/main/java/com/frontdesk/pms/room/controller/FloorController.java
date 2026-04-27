package com.frontdesk.pms.room.controller;

import com.frontdesk.pms.room.dto.FloorRequestDTO;
import com.frontdesk.pms.room.dto.FloorResponseDTO;
import com.frontdesk.pms.room.service.FloorService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/floors")
@RequiredArgsConstructor
public class FloorController {

    private final FloorService service;

    @PostMapping
    public ResponseEntity<FloorResponseDTO> createFloor(
            @Valid @RequestBody FloorRequestDTO request) {

        return ResponseEntity.ok(service.createFloor(request));
    }

    // Get all floors
    @GetMapping
    public ResponseEntity<List<FloorResponseDTO>> getAllFloors() {
        return ResponseEntity.ok(service.getAllFloors());
    }

    // Get floor by ID
    @GetMapping("/{id}")
    public ResponseEntity<FloorResponseDTO> getFloorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getFloorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FloorResponseDTO> updateFloor(
            @PathVariable Long id,
            @RequestBody FloorRequestDTO request) {

        return ResponseEntity.ok(service.updateFloor(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFloor(@PathVariable Long id) {

        service.deleteFloor(id);
        return ResponseEntity.ok("Floor deleted successfully");
    }
}