package com.pms.property.publish.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.domain.room.entity.PropertyAreaEntity;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.service.DraftService;
import com.pms.property.publish.mapper.PublishMapper;
import com.pms.property.publish.validator.PublishValidator;
import com.pms.property.upload.service.LocalImageStorageService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublishServiceTest {

    @Test
    void shouldRepublishIntoSamePropertyIdAndReplaceNormalizedRows() {
        DraftService draftService = mock(DraftService.class);
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        PropertyOverviewRepository propertyOverviewRepository = mock(PropertyOverviewRepository.class);
        GuestServiceAmenityRepository guestServiceAmenityRepository = mock(GuestServiceAmenityRepository.class);
        NearbyLocationAccessibilityRepository nearbyLocationAccessibilityRepository = mock(NearbyLocationAccessibilityRepository.class);
        PropertyAreaRepository propertyAreaRepository = mock(PropertyAreaRepository.class);
        RoomOutletTypeRepository roomOutletTypeRepository = mock(RoomOutletTypeRepository.class);
        FloorConfigurationRepository floorConfigurationRepository = mock(FloorConfigurationRepository.class);
        FloorPropertyAreaRepository floorPropertyAreaRepository = mock(FloorPropertyAreaRepository.class);
        InventoryRoomRepository inventoryRoomRepository = mock(InventoryRoomRepository.class);
        ChartOfAccountRepository chartOfAccountRepository = mock(ChartOfAccountRepository.class);
        RevenueMappingRepository revenueMappingRepository = mock(RevenueMappingRepository.class);
        PaymentMethodRepository paymentMethodRepository = mock(PaymentMethodRepository.class);
        TaxRuleRepository taxRuleRepository = mock(TaxRuleRepository.class);
        PublishMapper publishMapper = mock(PublishMapper.class);
        PublishValidator publishValidator = mock(PublishValidator.class);
        LocalImageStorageService localImageStorageService = mock(LocalImageStorageService.class);

        PublishService service = new PublishService(
            draftService,
            propertyRepository,
            propertyOverviewRepository,
            guestServiceAmenityRepository,
            nearbyLocationAccessibilityRepository,
            propertyAreaRepository,
            roomOutletTypeRepository,
            floorConfigurationRepository,
            floorPropertyAreaRepository,
            inventoryRoomRepository,
            chartOfAccountRepository,
            revenueMappingRepository,
            paymentMethodRepository,
            taxRuleRepository,
            publishMapper,
            publishValidator,
            localImageStorageService
        );

        PropertyDraftEntity draft = new PropertyDraftEntity();
        draft.setId(41L);
        draft.setStatus(DraftStatus.PUBLISHED);
        draft.setPublishedPropertyId("P-100");
        draft.setWizardData("{}");

        PropertyEntity existingProperty = new PropertyEntity();
        existingProperty.setId("P-100");
        existingProperty.setCreatedBy("owner");
        existingProperty.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        PropertyEntity mappedProperty = new PropertyEntity();
        mappedProperty.setTitle("Updated title");
        mappedProperty.setCreatedAt(Instant.now());

        PropertyOverviewEntity overview = new PropertyOverviewEntity();
        overview.setPropertyDescription("updated overview");

        PropertyOverviewEntity existingOverview = new PropertyOverviewEntity();
        existingOverview.setPropertyHeroImage("/uploads/old-hero.png");

        PropertyAreaEntity area = new PropertyAreaEntity();
        area.setAreaName("Lobby");

        ChartOfAccountEntity coa = new ChartOfAccountEntity();
        coa.setAccountCode("AC-1");

        PublishMapper.NormalizedPublishData normalized = new PublishMapper.NormalizedPublishData(
            new ObjectMapper().createObjectNode(),
            mappedProperty,
            overview,
            List.of(),
            List.of(),
            List.of(area),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(coa),
            List.of(),
            List.of(),
            List.of()
        );

        when(draftService.getById(41L)).thenReturn(draft);
        when(publishMapper.toNormalized("{}")).thenReturn(normalized);
        when(propertyRepository.findById("P-100")).thenReturn(java.util.Optional.of(existingProperty));
        when(propertyRepository.save(any(PropertyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(propertyOverviewRepository.findByPropertyId("P-100")).thenReturn(java.util.Optional.of(existingOverview));

        PropertyAreaEntity existingArea = new PropertyAreaEntity();
        existingArea.setImagesCsv("/uploads/old-area.png");
        when(propertyAreaRepository.findAllByPropertyId("P-100")).thenReturn(List.of(existingArea));

        com.pms.property.domain.room.entity.RoomOutletTypeEntity existingRoomType = new com.pms.property.domain.room.entity.RoomOutletTypeEntity();
        existingRoomType.setImagesCsv("/uploads/old-room.png");
        when(roomOutletTypeRepository.findAllByPropertyId("P-100")).thenReturn(List.of(existingRoomType));

        var response = service.publish(41L, "owner");

        assertEquals("P-100", response.propertyId());
        assertEquals("PUBLISHED", response.status());

        ArgumentCaptor<PropertyEntity> propertyCaptor = ArgumentCaptor.forClass(PropertyEntity.class);
        verify(propertyRepository).save(propertyCaptor.capture());
        assertEquals("P-100", propertyCaptor.getValue().getId());
        assertEquals("owner", propertyCaptor.getValue().getCreatedBy());
        assertEquals(existingProperty.getCreatedAt(), propertyCaptor.getValue().getCreatedAt());
        assertEquals("Updated title", propertyCaptor.getValue().getTitle());

        verify(propertyAreaRepository).deleteByPropertyId("P-100");
        verify(propertyOverviewRepository).deleteByPropertyId("P-100");
        verify(localImageStorageService).deleteByPublicUrl("/uploads/old-hero.png");
        verify(localImageStorageService).deleteByPublicUrl("/uploads/old-area.png");
        verify(localImageStorageService).deleteByPublicUrl("/uploads/old-room.png");
        verify(propertyOverviewRepository).flush();
        verify(draftService).markPublished(eq(draft), eq("P-100"), eq("owner"));
    }
}

