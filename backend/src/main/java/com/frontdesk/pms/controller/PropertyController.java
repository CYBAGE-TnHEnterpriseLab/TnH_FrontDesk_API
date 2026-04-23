package com.frontdesk.pms.controller;

import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // Create Property (Step 1: Property Details)
    @PostMapping
    public ResponseEntity<PropertyResponseDTO> createProperty(
            @Valid @RequestBody PropertyRequestDTO requestDTO) {

        PropertyResponseDTO response = propertyService.createProperty(requestDTO);
        return ResponseEntity.ok(response);
    }

    // Get all properties
    @GetMapping
    public ResponseEntity<List<PropertyResponseDTO>> getAllProperties() {
        return ResponseEntity.ok(propertyService.getAllProperties());
    }

// Get property by ID
    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponseDTO> getPropertyById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }
    
}
