
    
package com.frontdesk.pms.rate_management.controller;

import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.exception.MasterRoomNotFoundException;
import com.frontdesk.pms.rate_management.service.MasterRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/master-rooms")
public class MasterRoomController {
    @Autowired
    private MasterRoomService masterRoomService;


    @PostMapping
    public ResponseEntity<MasterRoomResponseDTO> createOrUpdateMasterRoom(@RequestBody MasterRoomRequestDTO masterRoomRequestDTO) {
        MasterRoomResponseDTO saved = masterRoomService.createOrUpdateMasterRoom(masterRoomRequestDTO);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/{id}")
    public ResponseEntity<MasterRoomResponseDTO> getMasterRoom(@PathVariable Long id) {
        return masterRoomService.getMasterRoom(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MasterRoomNotFoundException(id));
    }


    @GetMapping
    public List<MasterRoomResponseDTO> getAllMasterRooms() {
        return masterRoomService.getAllMasterRooms();
    }

    // Get pricing for a specific room type (inherited or overridden)
    @GetMapping("/room-type/{roomTypeId}/pricing")
    public List<MasterRoomPricingResponseDTO> getPricingByRoomType(@PathVariable Long roomTypeId) {
        return masterRoomService.getPricingByRoomType(roomTypeId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMasterRoom(@PathVariable Long id) {
        masterRoomService.deleteMasterRoom(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/pricing")
    public ResponseEntity<MasterRoomPricingResponseDTO> addOrUpdatePricing(@PathVariable Long id, @RequestBody MasterRoomPricingRequestDTO pricingRequestDTO) {
        MasterRoomPricingResponseDTO saved = masterRoomService.addOrUpdatePricing(id, pricingRequestDTO);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/{id}/pricing")
    public List<MasterRoomPricingResponseDTO> getPricingByMasterRoom(@PathVariable Long id) {
        return masterRoomService.getPricingByMasterRoom(id);
    }


    @PostMapping("/{id}/map-room-type")
    public ResponseEntity<MasterRoomRoomTypeMappingResponseDTO> mapRoomType(@PathVariable Long id, @RequestBody MasterRoomRoomTypeMappingRequestDTO mappingRequestDTO) {
        MasterRoomRoomTypeMappingResponseDTO saved = masterRoomService.mapRoomType(id, mappingRequestDTO);
        return ResponseEntity.ok(saved);
    }


    @GetMapping("/{id}/mappings")
    public List<MasterRoomRoomTypeMappingResponseDTO> getMappingsByMasterRoom(@PathVariable Long id) {
        return masterRoomService.getMappingsByMasterRoom(id);
    }

    @PostMapping("/validate-mappings")
    public ResponseEntity<Boolean> validateMappings(@RequestBody List<Long> activeRoomTypeIds) {
        boolean allMapped = masterRoomService.isAllRoomTypesMapped(activeRoomTypeIds);
        return ResponseEntity.ok(allMapped);
    }

            // Manual override: set a specific price for a room type and occupancy
    @PostMapping("/room-type/{roomTypeId}/override-pricing")
    public ResponseEntity<Void> overrideRoomTypePricing(@PathVariable Long roomTypeId, @RequestParam String occupancyType, @RequestParam Double newPrice) {
        masterRoomService.overrideRoomTypePricing(roomTypeId, occupancyType, newPrice);
        return ResponseEntity.ok().build();
    }

    // Break inheritance for all pricing of a room type
    @PostMapping("/room-type/{roomTypeId}/break-inheritance")
    public ResponseEntity<Void> breakInheritanceForRoomType(@PathVariable Long roomTypeId) {
        masterRoomService.breakInheritanceForRoomType(roomTypeId);
        return ResponseEntity.ok().build();
    }

    
}
