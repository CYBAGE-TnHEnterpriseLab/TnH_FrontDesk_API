package com.frontdesk.pms.content.controller;

import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestOptionDTO;
import com.frontdesk.pms.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ContentController {

    private final ContentService contentService;

    // Unified GET endpoint for all content (amenities, special requests, contact info)
    @GetMapping("/properties/{propertyId}/content")
    public ContentConfigurationResponseDTO getPropertyContent(@PathVariable UUID propertyId) {
        log.debug("Fetching unified content for propertyId={}", propertyId);
        return contentService.getContentConfiguration(propertyId);
    }

    @PutMapping("/properties/{propertyId}/content")
    public ContentConfigurationResponseDTO updatePropertyContent(
            @PathVariable UUID propertyId,
            @RequestBody @Valid ContentConfigurationResponseDTO request
    ) {
        log.info("Updating unified content for propertyId={}", propertyId);
        return contentService.upsertContentConfiguration(propertyId, request);
    }

    @PostMapping("/properties/{propertyId}/content")
    public ContentConfigurationResponseDTO createPropertyContent(
            @PathVariable UUID propertyId,
            @RequestBody @Valid ContentConfigurationResponseDTO request
    ) {
        log.info("Creating unified content for propertyId={}", propertyId);
        return contentService.createContentConfiguration(propertyId, request);
    }

    // Optionally, keep the endpoint for special request options if needed
    @GetMapping("/special-requests/options")
    public List<SpecialRequestOptionDTO> getSpecialRequestOptions() {
        log.debug("Fetching predefined special request options");
        return contentService.getSpecialRequestOptions();
    }
}
