
    
package com.frontdesk.pms.rate_management.controller;

import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.dto.PropertyRoomTypeMappingResponseDTO;
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


    // Create a new master room under a specific property.
    @PostMapping("/create-master-room/property/{propertyId}")
    public ResponseEntity<MasterRoomResponseDTO> createMasterRoom(@PathVariable String propertyId, @RequestBody MasterRoomRequestDTO masterRoomRequestDTO) {
        MasterRoomResponseDTO saved = masterRoomService.createMasterRoom(propertyId, masterRoomRequestDTO);
        return ResponseEntity.ok(saved);
    }

    // Update an existing master room within a specific property.
    @PutMapping("/update-master-room/property/{propertyId}/{id}")
    public ResponseEntity<MasterRoomResponseDTO> updateMasterRoom(@PathVariable String propertyId, @PathVariable Long id, @RequestBody MasterRoomRequestDTO masterRoomRequestDTO) {
        MasterRoomResponseDTO saved = masterRoomService.updateMasterRoom(propertyId, id, masterRoomRequestDTO);
        return ResponseEntity.ok(saved);
    }

    // List all master rooms configured for a property.
    @GetMapping("/get-all-master-room/property/{propertyId}")
    public List<MasterRoomResponseDTO> getMasterRoomsByProperty(@PathVariable String propertyId) {
        return masterRoomService.getMasterRoomsByPropertyId(propertyId);
    }

    // Delete a master room scoped to a property.
    @DeleteMapping("/delete-master-room/property/{propertyId}/{id}")
    public ResponseEntity<Void> deleteMasterRoom(@PathVariable String propertyId, @PathVariable Long id) {
        masterRoomService.deleteMasterRoom(propertyId, id);
        return ResponseEntity.noContent().build();
    }

    // Add or update master pricing for a specific occupancy type.
    @PostMapping("/update-pricing-by-occupancy/property/{propertyId}/{id}/pricing")
    public ResponseEntity<MasterRoomPricingResponseDTO> addOrUpdatePricing(@PathVariable String propertyId, @PathVariable Long id, @RequestBody MasterRoomPricingRequestDTO pricingRequestDTO) {
        MasterRoomPricingResponseDTO saved = masterRoomService.addOrUpdatePricing(propertyId, id, pricingRequestDTO);
        return ResponseEntity.ok(saved);
    }

    // Get all pricing rows directly associated with a master room.
    @GetMapping("/{id}/pricing")
    public List<MasterRoomPricingResponseDTO> getPricingByMasterRoom(@PathVariable Long id) {
        return masterRoomService.getPricingByMasterRoom(id);
    }

    // Map a room type to a master room and inherit pricing.
    @PostMapping("/map-room-type/property/{propertyId}/{id}")
    public ResponseEntity<MasterRoomRoomTypeMappingResponseDTO> mapRoomType(@PathVariable String propertyId, @PathVariable Long id, @RequestBody MasterRoomRoomTypeMappingRequestDTO mappingRequestDTO) {
        MasterRoomRoomTypeMappingResponseDTO saved = masterRoomService.upsertRoomTypeMapping(propertyId, mappingRequestDTO.getRoomTypeId(), id);
        return ResponseEntity.ok(saved);
    }

    // Get all room-type mappings for a master room.
    @GetMapping("/get-room-type-mapping/{id}/mappings")
    public List<MasterRoomRoomTypeMappingResponseDTO> getMappingsByMasterRoom(@PathVariable Long id) {
        return masterRoomService.getMappingsByMasterRoom(id);
    }

    // Get all room-type mappings for a property including inherited rates.
    @GetMapping("/property/{propertyId}/mappings")
    public List<PropertyRoomTypeMappingResponseDTO> getMappingsByProperty(@PathVariable String propertyId) {
        return masterRoomService.getMappingsByPropertyId(propertyId);
    }

    // Validate whether all active room types are mapped.
    @PostMapping("/validate-mappings")
    public ResponseEntity<Boolean> validateMappings(@RequestBody List<Long> activeRoomTypeIds) {
        boolean allMapped = masterRoomService.isAllRoomTypesMapped(activeRoomTypeIds);
        return ResponseEntity.ok(allMapped);
    }

    // Manually override pricing for one room type and occupancy.
    @PostMapping("/room-type/{roomTypeId}/override-pricing")
    public ResponseEntity<Void> overrideRoomTypePricing(@PathVariable Long roomTypeId, @RequestParam String occupancyType, @RequestParam Double newPrice) {
        masterRoomService.overrideRoomTypePricing(roomTypeId, occupancyType, newPrice);
        return ResponseEntity.ok().build();
    }

    // Break inherited pricing links for all pricing rows of a room type.
    @PostMapping("/room-type/{roomTypeId}/break-inheritance")
    public ResponseEntity<Void> breakInheritanceForRoomType(@PathVariable Long roomTypeId) {
        masterRoomService.breakInheritanceForRoomType(roomTypeId);
        return ResponseEntity.ok().build();
    }

    
}
