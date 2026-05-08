
package com.frontdesk.pms.content.service;

import com.frontdesk.common.dto.PropertyDTO;
import com.frontdesk.pms.content.dto.AmenitiesRequestDTO;
import com.frontdesk.pms.content.dto.AmenitiesResponseDTO;
import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestOptionDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsRequestDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsResponseDTO;
import com.frontdesk.pms.content.entity.PropertyAmenitiesConfiguration;
import com.frontdesk.pms.content.entity.PropertySpecialRequestsConfiguration;
import com.frontdesk.pms.content.exception.PropertyNotFoundException;
import com.frontdesk.pms.content.mapper.ContentMapper;
import com.frontdesk.pms.content.repository.PropertyAmenitiesConfigurationRepository;
import com.frontdesk.pms.content.repository.PropertySpecialRequestsConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

    private final PropertyLookupService propertyLookupService;
    private final PropertyServiceClient propertyServiceClient;
    private final PropertyAmenitiesConfigurationRepository amenitiesRepository;
    private final PropertySpecialRequestsConfigurationRepository specialRequestsRepository;

    public ContentServiceImpl(
            PropertyLookupService propertyLookupService,
            PropertyServiceClient propertyServiceClient,
            PropertyAmenitiesConfigurationRepository amenitiesRepository,
            PropertySpecialRequestsConfigurationRepository specialRequestsRepository
    ) {
        this.propertyLookupService = propertyLookupService;
        this.propertyServiceClient = propertyServiceClient;
        this.amenitiesRepository = amenitiesRepository;
        this.specialRequestsRepository = specialRequestsRepository;
    }

    @Override
    public ContentConfigurationResponseDTO getContentConfiguration(UUID propertyId) {
        assertPropertyExists(propertyId);
        PropertyDTO property = getExistingProperty(propertyId);
        return ContentMapper.toContentConfigurationResponse(
                property,
                getOrCreateSpecialRequestsConfiguration(propertyId),
                getOrCreateAmenitiesConfiguration(propertyId)
        );
    }

    @Override
    public SpecialRequestsResponseDTO getSpecialRequests(UUID propertyId) {
        assertPropertyExists(propertyId);
        return ContentMapper.toSpecialRequestsResponse(getOrCreateSpecialRequestsConfiguration(propertyId));
    }

    @Override
    public SpecialRequestsResponseDTO upsertSpecialRequests(UUID propertyId, SpecialRequestsRequestDTO request) {
        assertPropertyExists(propertyId);

        PropertySpecialRequestsConfiguration entity = specialRequestsRepository.findByPropertyId(propertyId)
                .orElseGet(() -> newSpecialRequestsConfiguration(propertyId));

        entity.setExtraPillowEnabled(request.getExtraPillowEnabled());
        entity.setBabyCribEnabled(request.getBabyCribEnabled());
        entity.setLateCheckOutEnabled(request.getLateCheckOutEnabled());
        entity.setHypoallergenicBeddingEnabled(request.getHypoallergenicBeddingEnabled());
        entity.setAirportPickupEnabled(request.getAirportPickupEnabled());
        entity.setWheelchairAccessEnabled(request.getWheelchairAccessEnabled());
        touch(entity);

        PropertySpecialRequestsConfiguration saved = specialRequestsRepository.save(entity);
        log.info("Updated special requests configuration for propertyId={}", propertyId);
        return ContentMapper.toSpecialRequestsResponse(saved, propertyId);
    }

    @Override
    public AmenitiesResponseDTO getAmenities(UUID propertyId) {
        assertPropertyExists(propertyId);
        return ContentMapper.toAmenitiesResponse(getOrCreateAmenitiesConfiguration(propertyId));
    }

    @Override
    public AmenitiesResponseDTO upsertAmenities(UUID propertyId, AmenitiesRequestDTO request) {
        assertPropertyExists(propertyId);

        PropertyAmenitiesConfiguration entity = amenitiesRepository.findByPropertyId(propertyId)
                .orElseGet(() -> newAmenitiesConfiguration(propertyId));

        entity.setAirportCode(normalize(request.getAirportCode(), true));
        entity.setDistanceJourneyTime(normalize(request.getDistanceJourneyTime(), false));
        entity.setDirections(normalize(request.getDirections(), false));
        entity.setGroundTransportEnabled(request.getGroundTransportEnabled());
        entity.setShuttleServiceEnabled(request.getShuttleServiceEnabled());
        entity.setSwimmingPoolEnabled(request.getSwimmingPoolEnabled());
        touch(entity);

        PropertyAmenitiesConfiguration saved = amenitiesRepository.save(entity);
        log.info("Updated amenities configuration for propertyId={}", propertyId);
        return ContentMapper.toAmenitiesResponse(saved, propertyId);
    }

    @Override
    public List<SpecialRequestOptionDTO> getSpecialRequestOptions() {
        return ContentMapper.predefinedOptions();
    }

    private void assertPropertyExists(UUID propertyId) {
        try {
            if (propertyLookupService.exists(propertyId)) {
                return;
            }
            throw new PropertyNotFoundException(propertyId);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to validate property because property-service is unavailable",
                    ex
            );
        }
    }

    @Override
    public ContentConfigurationResponseDTO upsertContentConfiguration(UUID propertyId, ContentConfigurationResponseDTO request) {
        assertPropertyExists(propertyId);
        // Update special requests
        if (request.getSpecialRequests() != null) {
            SpecialRequestsRequestDTO specialReq = new SpecialRequestsRequestDTO();
            specialReq.setExtraPillowEnabled(request.getSpecialRequests().isExtraPillowEnabled());
            specialReq.setBabyCribEnabled(request.getSpecialRequests().isBabyCribEnabled());
            specialReq.setLateCheckOutEnabled(request.getSpecialRequests().isLateCheckOutEnabled());
            specialReq.setHypoallergenicBeddingEnabled(request.getSpecialRequests().isHypoallergenicBeddingEnabled());
            specialReq.setAirportPickupEnabled(request.getSpecialRequests().isAirportPickupEnabled());
            specialReq.setWheelchairAccessEnabled(request.getSpecialRequests().isWheelchairAccessEnabled());
            upsertSpecialRequests(propertyId, specialReq);
        }
        // Update amenities
        if (request.getAmenities() != null) {
            AmenitiesRequestDTO amenitiesReq = new AmenitiesRequestDTO();
            amenitiesReq.setAirportCode(request.getAmenities().getAirportCode());
            amenitiesReq.setDistanceJourneyTime(request.getAmenities().getDistanceJourneyTime());
            amenitiesReq.setDirections(request.getAmenities().getDirections());
            amenitiesReq.setGroundTransportEnabled(request.getAmenities().isGroundTransportEnabled());
            amenitiesReq.setShuttleServiceEnabled(request.getAmenities().isShuttleServiceEnabled());
            amenitiesReq.setSwimmingPoolEnabled(request.getAmenities().isSwimmingPoolEnabled());
            upsertAmenities(propertyId, amenitiesReq);
        }
        // Return the updated config with contact info
        PropertyDTO property = getExistingProperty(propertyId);
        return ContentMapper.toContentConfigurationResponse(
                property,
                getOrCreateSpecialRequestsConfiguration(propertyId),
                getOrCreateAmenitiesConfiguration(propertyId)
        );
    }

    @Override
    public ContentConfigurationResponseDTO createContentConfiguration(UUID propertyId, ContentConfigurationResponseDTO request) {
        // For create, just delegate to upsert (idempotent for this use case)
        return upsertContentConfiguration(propertyId, request);
    }

    private PropertySpecialRequestsConfiguration getOrCreateSpecialRequestsConfiguration(UUID propertyId) {
        return specialRequestsRepository.findByPropertyId(propertyId)
                .orElseGet(() -> newSpecialRequestsConfiguration(propertyId));
    }

    private PropertyDTO getExistingProperty(UUID propertyId) {
        try {
            PropertyDTO property = propertyServiceClient.getPropertyDetails(propertyId);
            if (property == null) {
                throw new PropertyNotFoundException(propertyId);
            }
            return property;
        } catch (PropertyNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to fetch property details because property-service is unavailable",
                    ex
            );
        }
    }

    private PropertyAmenitiesConfiguration getOrCreateAmenitiesConfiguration(UUID propertyId) {
        return amenitiesRepository.findByPropertyId(propertyId)
                .orElseGet(() -> newAmenitiesConfiguration(propertyId));
    }

    private PropertySpecialRequestsConfiguration newSpecialRequestsConfiguration(UUID propertyId) {
        PropertySpecialRequestsConfiguration entity = new PropertySpecialRequestsConfiguration();
        entity.setPropertyId(propertyId);
        touch(entity);
        return entity;
    }

    private PropertyAmenitiesConfiguration newAmenitiesConfiguration(UUID propertyId) {
        PropertyAmenitiesConfiguration entity = new PropertyAmenitiesConfiguration();
        entity.setPropertyId(propertyId);
        touch(entity);
        return entity;
    }

    private void touch(com.frontdesk.common.entity.BaseEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private String normalize(String value, boolean uppercase) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return uppercase ? trimmed.toUpperCase() : trimmed;
    }
}
