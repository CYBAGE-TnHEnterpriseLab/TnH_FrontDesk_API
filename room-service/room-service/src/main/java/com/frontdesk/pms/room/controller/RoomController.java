package com.frontdesk.pms.room.controller;

import com.frontdesk.pms.room.dto.RoomRequestDTO;
import com.frontdesk.pms.room.dto.RoomResponseDTO;
import com.frontdesk.pms.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService service;

    @PostMapping
    public ResponseEntity<List<RoomResponseDTO>> createRooms(
            @Valid @RequestBody RoomRequestDTO request) {

        return ResponseEntity.ok(service.createRooms(request));
    }
    // Get all rooms
    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        return ResponseEntity.ok(service.getAllRooms());
    }

    // Get rooms by floor
    @GetMapping("/floor/{floorId}")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByFloor(
            @PathVariable Long floorId,
            @RequestParam Long propertyId) {

        return ResponseEntity.ok(service.getRoomsByFloor(floorId, propertyId));
    }

    // Get rooms by property
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByProperty(
            @PathVariable Long propertyId) {

        return ResponseEntity.ok(service.getRoomsByProperty(propertyId));
    }

    // Get rooms by room type
    @GetMapping("/type/{roomTypeId}")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByType(
            @PathVariable Long roomTypeId) {

        return ResponseEntity.ok(service.getRoomsByType(roomTypeId));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponseDTO> updateRoom(
            @PathVariable Long roomId,
            @RequestParam Long roomTypeId) {

        return ResponseEntity.ok(service.updateRoom(roomId, roomTypeId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long roomId) {
            service.deleteRoom(roomId);
        return ResponseEntity.ok("Room deleted successfully");
    }

    
}
