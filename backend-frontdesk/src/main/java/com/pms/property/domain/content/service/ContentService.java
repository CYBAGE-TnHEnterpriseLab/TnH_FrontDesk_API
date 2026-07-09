package com.pms.property.domain.content.service;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.content.dto.ContentOverviewRequest;
import com.pms.property.domain.content.dto.ContentOverviewResponse;
import com.pms.property.domain.content.dto.ContentSummaryResponse;
import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final PropertyOverviewRepository propertyOverviewRepository;
    private final GuestServiceAmenityRepository guestServiceAmenityRepository;
    private final NearbyLocationAccessibilityRepository nearbyLocationAccessibilityRepository;

    public ContentService(
        PropertyOverviewRepository propertyOverviewRepository,
        GuestServiceAmenityRepository guestServiceAmenityRepository,
        NearbyLocationAccessibilityRepository nearbyLocationAccessibilityRepository
    ) {
        this.propertyOverviewRepository = propertyOverviewRepository;
        this.guestServiceAmenityRepository = guestServiceAmenityRepository;
        this.nearbyLocationAccessibilityRepository = nearbyLocationAccessibilityRepository;
    }

    @Transactional(readOnly = true)
    public ContentSummaryResponse getSummaryByPropertyId(String propertyId) {
        PropertyOverviewEntity overview = propertyOverviewRepository.findByPropertyId(propertyId).orElse(null);

        long amenitiesCount = guestServiceAmenityRepository.countByPropertyId(propertyId);
        long nearbyCount = nearbyLocationAccessibilityRepository.countByPropertyId(propertyId);

        return new ContentSummaryResponse(
            propertyId,
            overview != null ? overview.getPropertyDescription() : "",
            overview != null ? overview.getPropertyHeroImage() : "",
            amenitiesCount,
            nearbyCount
        );
    }

    @Transactional(readOnly = true)
    public List<ContentOverviewResponse> listOverviewsByPropertyId(String propertyId) {
        return propertyOverviewRepository.findAllByPropertyId(propertyId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContentOverviewResponse getOverviewById(String propertyId, Long overviewId) {
        return propertyOverviewRepository.findByPropertyIdAndId(propertyId, overviewId)
            .map(this::toResponse)
            .orElseThrow(() -> new NotFoundException("Content overview not found: " + overviewId));
    }

    @Transactional
    public ContentOverviewResponse createOverview(String propertyId, ContentOverviewRequest request) {
        if (propertyOverviewRepository.findByPropertyId(propertyId).isPresent()) {
            throw new BadRequestException("Content overview already exists for property: " + propertyId);
        }

        PropertyOverviewEntity entity = new PropertyOverviewEntity();
        entity.setPropertyId(propertyId);
        entity.setPropertyHeroImage(request.propertyHeroImage());
        entity.setPropertyDescription(request.propertyDescription());
        return toResponse(propertyOverviewRepository.save(entity));
    }

    @Transactional
    public ContentOverviewResponse updateOverview(String propertyId, Long overviewId, ContentOverviewRequest request) {
        PropertyOverviewEntity entity = propertyOverviewRepository.findByPropertyIdAndId(propertyId, overviewId)
            .orElseThrow(() -> new NotFoundException("Content overview not found: " + overviewId));
        entity.setPropertyHeroImage(request.propertyHeroImage());
        entity.setPropertyDescription(request.propertyDescription());
        return toResponse(propertyOverviewRepository.save(entity));
    }

    @Transactional
    public void deleteOverview(String propertyId, Long overviewId) {
        PropertyOverviewEntity entity = propertyOverviewRepository.findByPropertyIdAndId(propertyId, overviewId)
            .orElseThrow(() -> new NotFoundException("Content overview not found: " + overviewId));
        propertyOverviewRepository.delete(entity);
    }

    private ContentOverviewResponse toResponse(PropertyOverviewEntity entity) {
        return new ContentOverviewResponse(
            entity.getId(),
            entity.getPropertyId(),
            entity.getPropertyHeroImage(),
            entity.getPropertyDescription()
        );
    }
}

