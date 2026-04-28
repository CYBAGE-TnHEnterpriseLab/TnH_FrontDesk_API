package com.frontdesk.pms.room.controller;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.service.RoomTypeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService service;

    // Create Room Type
    @PostMapping
    public ResponseEntity<RoomTypeResponseDTO> createRoomType(
            @Valid @RequestBody RoomTypeRequestDTO request) {

        RoomTypeResponseDTO response = service.createRoomType(request);
        return ResponseEntity.ok(response);
    }

    // Get all room types
    @GetMapping
    public ResponseEntity<List<RoomTypeResponseDTO>> getAllRoomTypes() {
        return ResponseEntity.ok(service.getAllRoomTypes());
    }

    // Get room type by ID
    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeResponseDTO> getRoomTypeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getRoomTypeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeResponseDTO> updateRoomType(
            @PathVariable Long id,
            @RequestBody RoomTypeRequestDTO request) {

        return ResponseEntity.ok(service.updateRoomType(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoomType(@PathVariable Long id) {

        service.deleteRoomType(id);
        return ResponseEntity.ok("Room type deleted successfully");
    }
}