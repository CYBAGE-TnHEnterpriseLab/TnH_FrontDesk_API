package com.pms.property.domain.room.controller;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.room.dto.InventoryRoomRequest;
import com.pms.property.domain.room.dto.InventoryRoomResponse;
import com.pms.property.domain.room.dto.RoomOutletTypeResponse;
import com.pms.property.domain.room.dto.RoomSummaryResponse;
import com.pms.property.domain.room.service.RoomService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** Fetches the room setup summary for a published property. */
    @GetMapping("/properties/{propertyId}/summary")
    public ResponseEntity<ApiResponse<RoomSummaryResponse>> getSummary(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getSummaryByPropertyId(propertyId), "Published property room summary fetched"));
    }

    /** Fetches inventory rooms configured for a published property. */
    @GetMapping("/properties/{propertyId}/inventory-rooms")
    public ResponseEntity<ApiResponse<List<InventoryRoomResponse>>> listInventoryRooms(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.listInventoryRoomsByPropertyId(propertyId), "Published property inventory rooms fetched"));
    }

    /** Fetches supported room outlet types for a published property. */
    @GetMapping("/properties/{propertyId}/room-outlet-types")
    public ResponseEntity<ApiResponse<List<RoomOutletTypeResponse>>> listRoomOutletTypes(@PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.listRoomOutletTypesByPropertyId(propertyId), "Published property room outlet types fetched"));
    }

    /** Fetches an inventory room by id for a published property. */
    @GetMapping("/properties/{propertyId}/inventory-rooms/{roomId}")
    public ResponseEntity<ApiResponse<InventoryRoomResponse>> getInventoryRoomById(
        @PathVariable String propertyId,
        @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getInventoryRoomById(propertyId, roomId), "Published property inventory room fetched"));
    }

    /** Creates an inventory room for a published property. */
    @PostMapping("/properties/{propertyId}/inventory-rooms")
    public ResponseEntity<ApiResponse<InventoryRoomResponse>> createInventoryRoom(
        @PathVariable String propertyId,
        @RequestBody InventoryRoomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.createInventoryRoom(propertyId, request), "Published property inventory room created"));
    }

    /** Updates an inventory room for a published property. */
    @PutMapping("/properties/{propertyId}/inventory-rooms/{roomId}")
    public ResponseEntity<ApiResponse<InventoryRoomResponse>> updateInventoryRoom(
        @PathVariable String propertyId,
        @PathVariable Long roomId,
        @RequestBody InventoryRoomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.updateInventoryRoom(propertyId, roomId, request), "Published property inventory room updated"));
    }

    /** Deletes an inventory room from a published property. */
    @DeleteMapping("/properties/{propertyId}/inventory-rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryRoom(
        @PathVariable String propertyId,
        @PathVariable Long roomId
    ) {
        roomService.deleteInventoryRoom(propertyId, roomId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Published property inventory room deleted"));
    }
}

