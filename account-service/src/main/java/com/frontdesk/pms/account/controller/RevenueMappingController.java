package com.frontdesk.pms.account.controller;

import com.frontdesk.pms.account.dto.RevenueMappingValidationResponseDTO;
import com.frontdesk.pms.account.dto.RevenueMappingRequestDTO;
import com.frontdesk.pms.account.dto.RevenueMappingResponseDTO;
import com.frontdesk.pms.account.service.RevenueMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/properties/{propertyId}/revenue-mappings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RevenueMappingController {

    private final RevenueMappingService service;

    @PostMapping
    public RevenueMappingResponseDTO create(
            @PathVariable UUID propertyId,
            @RequestBody @Valid RevenueMappingRequestDTO request
    ) {
        return service.create(propertyId, request);
    }

    @GetMapping
    public List<RevenueMappingResponseDTO> list(@PathVariable UUID propertyId) {
        return service.listByProperty(propertyId);
    }

    @GetMapping("/validation/posting-readiness")
    public RevenueMappingValidationResponseDTO validatePostingReadiness(@PathVariable UUID propertyId) {
        return service.validatePostingReadiness(propertyId);
    }

    @GetMapping("/{mappingId}")
    public RevenueMappingResponseDTO get(
            @PathVariable UUID propertyId,
            @PathVariable UUID mappingId
    ) {
        return service.get(propertyId, mappingId);
    }

    @PutMapping("/{mappingId}")
    public RevenueMappingResponseDTO update(
            @PathVariable UUID propertyId,
            @PathVariable UUID mappingId,
            @RequestBody @Valid RevenueMappingRequestDTO request
    ) {
        return service.update(propertyId, mappingId, request);
    }

    @DeleteMapping("/{mappingId}")
    public void delete(
            @PathVariable UUID propertyId,
            @PathVariable UUID mappingId
    ) {
        service.delete(propertyId, mappingId);
    }
}
