package com.frontdesk.pms.room.controller;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.service.RoomTypeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoomTypeController {

    private final RoomTypeService service;

    // Create Room Type for a property (propertyId only in path)
    @PostMapping("/property/{propertyId}")
    public ResponseEntity<RoomTypeResponseDTO> createRoomTypeForProperty(
            @PathVariable UUID propertyId,
            @Valid @RequestBody RoomTypeRequestDTO request) {
        request.setPropertyId(propertyId);
        RoomTypeResponseDTO response = service.createRoomType(request);
        return ResponseEntity.ok(response);
    }


    // Get all room types
    @GetMapping
    public ResponseEntity<List<RoomTypeResponseDTO>> getAllRoomTypes() {
        return ResponseEntity.ok(service.getAllRoomTypes());
    }

    // Get room types by propertyId (clear endpoint)
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RoomTypeResponseDTO>> getRoomTypesByPropertyId(@PathVariable java.util.UUID propertyId) {
        return ResponseEntity.ok(service.getRoomTypesByPropertyId(propertyId));
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