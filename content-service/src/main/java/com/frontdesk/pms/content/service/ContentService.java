package com.frontdesk.pms.content.service;

import com.frontdesk.pms.content.dto.AmenitiesRequestDTO;
import com.frontdesk.pms.content.dto.AmenitiesResponseDTO;
import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestOptionDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsRequestDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ContentService {
    ContentConfigurationResponseDTO getContentConfiguration(UUID propertyId);
    ContentConfigurationResponseDTO upsertContentConfiguration(UUID propertyId, ContentConfigurationResponseDTO request);
    ContentConfigurationResponseDTO createContentConfiguration(UUID propertyId, ContentConfigurationResponseDTO request);

    SpecialRequestsResponseDTO getSpecialRequests(UUID propertyId);
    SpecialRequestsResponseDTO upsertSpecialRequests(UUID propertyId, SpecialRequestsRequestDTO request);
    AmenitiesResponseDTO getAmenities(UUID propertyId);
    AmenitiesResponseDTO upsertAmenities(UUID propertyId, AmenitiesRequestDTO request);
    List<SpecialRequestOptionDTO> getSpecialRequestOptions();
}
