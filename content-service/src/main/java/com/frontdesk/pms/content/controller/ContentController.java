package com.frontdesk.pms.content.controller;

import com.frontdesk.pms.content.dto.AmenitiesRequestDTO;
import com.frontdesk.pms.content.dto.AmenitiesResponseDTO;
import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestOptionDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsRequestDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsResponseDTO;
import com.frontdesk.pms.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/special-requests/options")
    public List<SpecialRequestOptionDTO> getSpecialRequestOptions() {
        log.debug("Fetching predefined special request options");
        return contentService.getSpecialRequestOptions();
    }

    @GetMapping("/properties/{propertyId}")
    public ContentConfigurationResponseDTO getContentConfiguration(@PathVariable UUID propertyId) {
        log.debug("Fetching content configuration for propertyId={}", propertyId);
        return contentService.getContentConfiguration(propertyId);
    }

    @GetMapping("/properties/{propertyId}/special-requests")
    public SpecialRequestsResponseDTO getSpecialRequests(@PathVariable UUID propertyId) {
        log.debug("Fetching special requests configuration for propertyId={}", propertyId);
        return contentService.getSpecialRequests(propertyId);
    }

    @PutMapping("/properties/{propertyId}/special-requests")
    public SpecialRequestsResponseDTO updateSpecialRequests(
            @PathVariable UUID propertyId,
            @RequestBody @Valid SpecialRequestsRequestDTO request
    ) {
        log.info("Updating special requests configuration for propertyId={}", propertyId);
        return contentService.upsertSpecialRequests(propertyId, request);
    }

    @GetMapping("/properties/{propertyId}/amenities")
    public AmenitiesResponseDTO getAmenities(@PathVariable UUID propertyId) {
        log.debug("Fetching amenities configuration for propertyId={}", propertyId);
        return contentService.getAmenities(propertyId);
    }

    @PutMapping("/properties/{propertyId}/amenities")
    public AmenitiesResponseDTO updateAmenities(
            @PathVariable UUID propertyId,
            @RequestBody @Valid AmenitiesRequestDTO request
    ) {
        log.info("Updating amenities configuration for propertyId={}", propertyId);
        return contentService.upsertAmenities(propertyId, request);
    }
}
