package com.pms.property.domain.content.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.content.dto.ContentOverviewRequest;
import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentServiceTest {

    @Test
    void shouldCreateOverview() {
        PropertyOverviewRepository overviewRepository = mock(PropertyOverviewRepository.class);
        GuestServiceAmenityRepository amenityRepository = mock(GuestServiceAmenityRepository.class);
        NearbyLocationAccessibilityRepository nearbyRepository = mock(NearbyLocationAccessibilityRepository.class);
        ContentService service = new ContentServiceImpl(overviewRepository, amenityRepository, nearbyRepository);

        when(overviewRepository.findByPropertyId("P-1")).thenReturn(Optional.empty());
        when(overviewRepository.save(any(PropertyOverviewEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createOverview("P-1", new ContentOverviewRequest("hero.png", "Nice property"));

        assertEquals("P-1", response.propertyId());
        assertEquals("hero.png", response.propertyHeroImage());
    }

    @Test
    void shouldRejectDuplicateOverviewForProperty() {
        PropertyOverviewRepository overviewRepository = mock(PropertyOverviewRepository.class);
        GuestServiceAmenityRepository amenityRepository = mock(GuestServiceAmenityRepository.class);
        NearbyLocationAccessibilityRepository nearbyRepository = mock(NearbyLocationAccessibilityRepository.class);
        ContentService service = new ContentServiceImpl(overviewRepository, amenityRepository, nearbyRepository);

        when(overviewRepository.findByPropertyId("P-1")).thenReturn(Optional.of(new PropertyOverviewEntity()));

        assertThrows(BadRequestException.class, () -> service.createOverview("P-1", new ContentOverviewRequest("hero.png", "desc")));
    }
}

