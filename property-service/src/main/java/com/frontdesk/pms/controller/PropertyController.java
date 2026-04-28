package com.frontdesk.pms.controller;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.dto.PropertyRequestDTO;
import com.frontdesk.pms.dto.PropertyResponseDTO;
import com.frontdesk.pms.service.PropertyService;
import com.frontdesk.pms.validation.OnCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.groups.Default;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PropertyController {

    private final PropertyService service;

    @PostMapping
    public PropertyResponseDTO create(@RequestBody @Validated({Default.class, OnCreate.class}) PropertyRequestDTO request) {
        log.info("Create property request: name='{}', email='{}', timeZone='{}'", request.getName(), request.getEmail(), request.getTimeZone());
        return service.createDraft(request);
    }

    @PutMapping("/{propertyId}")
    public PropertyResponseDTO update(
            @PathVariable UUID propertyId,
            @RequestBody @Validated PropertyRequestDTO request
    ) {
        log.info("Update property request: propertyId={}, name='{}', email='{}', timeZone='{}'", propertyId, request.getName(), request.getEmail(), request.getTimeZone());
        return service.updateProperty(propertyId, request);
    }

    @DeleteMapping("/{propertyId}")
    public void delete(@PathVariable UUID propertyId) {
        log.info("Delete property request: propertyId={}", propertyId);
        service.deleteProperty(propertyId);
    }

    /**
     * Fetch a single property by UUID.
     *
     * Example:
     * - GET /api/properties/9c4c7a2b-2f8a-4f0d-9f0b-7d8a9f0c1a2b
     */
    @GetMapping("/{propertyId}")
    public PropertyResponseDTO findById(@PathVariable UUID propertyId) {
        log.debug("Find property by id request: propertyId={}", propertyId);
        return service.findPropertiesByUUID(propertyId);
    }

    /**
     * Fetch all properties.
     *
     * Example:
     * - GET /api/properties/all
     */
    @GetMapping("/all")
    public List<PropertyResponseDTO> findAll() {
        log.debug("Find all properties request");
        return service.getAllProperties();
    }

    /**
     * Find properties by exact name (case-insensitive).
     *
     * Examples:
     * - GET /api/properties/by-name?name=Frontdesk%20Beach%20Goa
     * - GET /api/properties/by-name?name=frontdesk%20beach%20goa
     */
    @GetMapping("/by-name")
    public List<PropertyResponseDTO> findByName(@RequestParam String name) {
        log.debug("Find properties by name request: name='{}'", name);
        return service.findPropertiesByName(name);
    }

    /**
     * Search/list properties with optional filters.
     *
     * Examples:
     * - GET /api/properties?name=Grand
     * - GET /api/properties?timeZone=Asia/Kolkata&checkInFrom=12:00:00&checkInTo=15:00:00
     */
    @GetMapping
    public List<PropertyResponseDTO> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkInTo,
            @RequestParam(required = false) PropertyStatus status
    ) {
        log.debug("Search properties: name='{}', timeZone='{}', checkInFrom={}, checkInTo={}, status={}",
                name, timeZone, checkInFrom, checkInTo, status);
        return service.searchProperties(name, timeZone, checkInFrom, checkInTo, status);
    }
}
