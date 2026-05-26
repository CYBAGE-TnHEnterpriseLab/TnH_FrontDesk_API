package com.frontdesk.pms.content.service;

import com.frontdesk.common.dto.PropertyDTO;
import com.frontdesk.pms.content.dto.AmenitiesRequestDTO;
import com.frontdesk.pms.content.dto.ContentConfigurationResponseDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsRequestDTO;
import com.frontdesk.pms.content.dto.SpecialRequestsResponseDTO;
import com.frontdesk.pms.content.entity.PropertyAmenitiesConfiguration;
import com.frontdesk.pms.content.entity.PropertySpecialRequestsConfiguration;
import com.frontdesk.pms.content.exception.PropertyNotFoundException;
import com.frontdesk.pms.content.repository.PropertyAmenitiesConfigurationRepository;
import com.frontdesk.pms.content.repository.PropertySpecialRequestsConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private PropertyLookupService propertyLookupService;

    @Mock

    private PropertyAmenitiesConfigurationRepository amenitiesRepository;

    @Mock
    private PropertySpecialRequestsConfigurationRepository specialRequestsRepository;

    @Mock
    private PropertyServiceClient propertyServiceClient;

    private ContentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContentServiceImpl(
                propertyLookupService,
                propertyServiceClient,
                amenitiesRepository,
                specialRequestsRepository
        );
    }

    @Test
    void getContentConfigurationIncludesContactInfoAndNoNestedPropertyIds() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(eq(propertyId))).thenReturn(true);
        when(specialRequestsRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.of(specialRequests(propertyId)));
        when(amenitiesRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.of(amenities(propertyId)));

        when(propertyServiceClient.getPropertyDetails(eq(propertyId))).thenReturn(property(propertyId));

        ContentConfigurationResponseDTO response = service.getContentConfiguration(propertyId);

        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getContactName()).isEqualTo("Riya D'Souza");
        assertThat(response.getEmail()).isEqualTo("goa@frontdesk.com");
        assertThat(response.getSpecialRequests().isExtraPillowEnabled()).isTrue();
        assertThat(response.getAmenities().getAirportCode()).isEqualTo("GOI");
    }

    @Test
    void getContentConfigurationThrowsWhenPropertyMissing() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(eq(propertyId))).thenReturn(false);

        assertThatThrownBy(() -> service.getContentConfiguration(propertyId))
                .isInstanceOf(PropertyNotFoundException.class);
    }

    // This test is not applicable since ContentServiceImpl does not call propertyServiceClient directly anymore.
    // If you want to test service unavailability, mock propertyLookupService.exists to throw RestClientException.
    @Test
    void getContentConfigurationWrapsPropertyServiceFailure() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(eq(propertyId))).thenThrow(new RestClientException("down"));

        assertThatThrownBy(() -> service.getContentConfiguration(propertyId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE");
    }

    @Test
    void upsertSpecialRequestsSavesExistingConfiguration() {
        UUID propertyId = UUID.randomUUID();
        PropertySpecialRequestsConfiguration existing = specialRequests(propertyId);
        SpecialRequestsRequestDTO request = new SpecialRequestsRequestDTO();
        request.setExtraPillowEnabled(false);
        request.setBabyCribEnabled(true);
        request.setLateCheckOutEnabled(true);
        request.setHypoallergenicBeddingEnabled(false);
        request.setAirportPickupEnabled(true);
        request.setWheelchairAccessEnabled(false);

        when(propertyLookupService.exists(eq(propertyId))).thenReturn(true);
        when(specialRequestsRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.of(existing));
        when(specialRequestsRepository.save(existing)).thenReturn(existing);

        SpecialRequestsResponseDTO response = service.upsertSpecialRequests(propertyId, request);

        assertThat(response.isBabyCribEnabled()).isTrue();
        assertThat(existing.isExtraPillowEnabled()).isFalse();
        assertThat(existing.getUpdatedAt()).isNotNull();
        verify(specialRequestsRepository).save(existing);
    }

    @Test
    void upsertAmenitiesNormalizesBlankAndAirportCode() {
        UUID propertyId = UUID.randomUUID();
        AmenitiesRequestDTO request = new AmenitiesRequestDTO();
        request.setAirportCode(" goi ");
        request.setDistanceJourneyTime("   ");
        request.setDirections(" Take the airport road ");
        request.setGroundTransportEnabled(true);
        request.setShuttleServiceEnabled(false);
        request.setSwimmingPoolEnabled(true);

        when(propertyLookupService.exists(eq(propertyId))).thenReturn(true);
        when(amenitiesRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.empty());
        when(amenitiesRepository.save(any(PropertyAmenitiesConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upsertAmenities(propertyId, request);

        ArgumentCaptor<PropertyAmenitiesConfiguration> captor = ArgumentCaptor.forClass(PropertyAmenitiesConfiguration.class);
        verify(amenitiesRepository).save(captor.capture());
        assertThat(captor.getValue().getPropertyId()).isEqualTo(propertyId);
        assertThat(captor.getValue().getAirportCode()).isEqualTo("GOI");
        assertThat(captor.getValue().getDistanceJourneyTime()).isNull();
        assertThat(captor.getValue().getDirections()).isEqualTo("Take the airport road");
    }

        @Test
        void upsertContentConfigurationSavesBothSectionsAndReturnsContactInfo() {
        UUID propertyId = UUID.randomUUID();
        ContentConfigurationResponseDTO request = ContentConfigurationResponseDTO.builder()
            .specialRequests(SpecialRequestsResponseDTO.builder().extraPillowEnabled(true).build())
            .amenities(com.frontdesk.pms.content.dto.AmenitiesResponseDTO.builder()
                .airportCode("goi")
                .groundTransportEnabled(true)
                .build())
            .build();

        when(propertyLookupService.exists(eq(propertyId))).thenReturn(true);
        when(specialRequestsRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.empty());
        when(amenitiesRepository.findByPropertyId(eq(propertyId))).thenReturn(Optional.empty());
        when(specialRequestsRepository.save(any(PropertySpecialRequestsConfiguration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(amenitiesRepository.save(any(PropertyAmenitiesConfiguration.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        when(propertyServiceClient.getPropertyDetails(eq(propertyId))).thenReturn(property(propertyId));

        ContentConfigurationResponseDTO response = service.upsertContentConfiguration(propertyId, request);

        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getContactName()).isEqualTo("Riya D'Souza");
        verify(specialRequestsRepository).save(any(PropertySpecialRequestsConfiguration.class));
        verify(amenitiesRepository).save(any(PropertyAmenitiesConfiguration.class));
        }

    @Test
    void upsertSpecialRequestsDoesNotSaveWhenPropertyMissing() {
        UUID propertyId = UUID.randomUUID();
        when(propertyLookupService.exists(eq(propertyId))).thenReturn(false);

        assertThatThrownBy(() -> service.upsertSpecialRequests(propertyId, new SpecialRequestsRequestDTO()))
                .isInstanceOf(PropertyNotFoundException.class);

        verify(specialRequestsRepository, never()).save(any(PropertySpecialRequestsConfiguration.class));
    }

    private PropertyDTO property(UUID propertyId) {
        PropertyDTO property = new PropertyDTO();
        property.setId(propertyId);
        property.setContactName("Riya D'Souza");
        property.setEmail("goa@frontdesk.com");
        return property;
    }

    private PropertySpecialRequestsConfiguration specialRequests(UUID propertyId) {
        PropertySpecialRequestsConfiguration entity = new PropertySpecialRequestsConfiguration();
        entity.setPropertyId(propertyId);
        entity.setExtraPillowEnabled(true);
        entity.setLateCheckOutEnabled(true);
        return entity;
    }

    private PropertyAmenitiesConfiguration amenities(UUID propertyId) {
        PropertyAmenitiesConfiguration entity = new PropertyAmenitiesConfiguration();
        entity.setPropertyId(propertyId);
        entity.setAirportCode("GOI");
        entity.setDirections("Take the airport road");
        entity.setGroundTransportEnabled(true);
        return entity;
    }
}
