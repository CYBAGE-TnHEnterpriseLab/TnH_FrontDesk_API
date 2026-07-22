package com.pms.property.publish.service;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.content.entity.GuestServiceAmenityEntity;
import com.pms.property.domain.content.entity.NearbyLocationAccessibilityEntity;
import com.pms.property.domain.content.entity.PropertyOverviewEntity;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import com.pms.property.domain.finance.entity.ChartOfAccountEntity;
import com.pms.property.domain.finance.entity.RevenueMappingEntity;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import com.pms.property.domain.payment.entity.PaymentMethodEntity;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.domain.room.entity.FloorConfigurationEntity;
import com.pms.property.domain.room.entity.FloorPropertyAreaEntity;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.entity.PropertyAreaEntity;
import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import com.pms.property.domain.tax.entity.TaxRuleEntity;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.service.DraftService;
import com.pms.property.integration.inventory.service.InventorySyncService;
import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.mapper.PublishMapper;
import com.pms.property.publish.validator.PublishValidator;
import com.pms.property.upload.service.LocalImageStorageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishServiceImpl implements PublishService {

    private final DraftService draftService;
    private final PropertyRepository propertyRepository;
    private final PropertyOverviewRepository propertyOverviewRepository;
    private final GuestServiceAmenityRepository guestServiceAmenityRepository;
    private final NearbyLocationAccessibilityRepository nearbyLocationAccessibilityRepository;
    private final PropertyAreaRepository propertyAreaRepository;
    private final RoomOutletTypeRepository roomOutletTypeRepository;
    private final FloorConfigurationRepository floorConfigurationRepository;
    private final FloorPropertyAreaRepository floorPropertyAreaRepository;
    private final InventoryRoomRepository inventoryRoomRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final RevenueMappingRepository revenueMappingRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final PublishMapper publishMapper;
    private final PublishValidator publishValidator;
    private final LocalImageStorageService localImageStorageService;
    private final InventorySyncService inventorySyncService;

    public PublishServiceImpl(
        DraftService draftService,
        PropertyRepository propertyRepository,
        PropertyOverviewRepository propertyOverviewRepository,
        GuestServiceAmenityRepository guestServiceAmenityRepository,
        NearbyLocationAccessibilityRepository nearbyLocationAccessibilityRepository,
        PropertyAreaRepository propertyAreaRepository,
        RoomOutletTypeRepository roomOutletTypeRepository,
        FloorConfigurationRepository floorConfigurationRepository,
        FloorPropertyAreaRepository floorPropertyAreaRepository,
        InventoryRoomRepository inventoryRoomRepository,
        ChartOfAccountRepository chartOfAccountRepository,
        RevenueMappingRepository revenueMappingRepository,
        PaymentMethodRepository paymentMethodRepository,
        TaxRuleRepository taxRuleRepository,
        PublishMapper publishMapper,
        PublishValidator publishValidator,
        LocalImageStorageService localImageStorageService,
        InventorySyncService inventorySyncService
    ) {
        this.draftService = draftService;
        this.propertyRepository = propertyRepository;
        this.propertyOverviewRepository = propertyOverviewRepository;
        this.guestServiceAmenityRepository = guestServiceAmenityRepository;
        this.nearbyLocationAccessibilityRepository = nearbyLocationAccessibilityRepository;
        this.propertyAreaRepository = propertyAreaRepository;
        this.roomOutletTypeRepository = roomOutletTypeRepository;
        this.floorConfigurationRepository = floorConfigurationRepository;
        this.floorPropertyAreaRepository = floorPropertyAreaRepository;
        this.inventoryRoomRepository = inventoryRoomRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.revenueMappingRepository = revenueMappingRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.publishMapper = publishMapper;
        this.publishValidator = publishValidator;
        this.localImageStorageService = localImageStorageService;
        this.inventorySyncService = inventorySyncService;
    }

    @Override
    @Transactional
    public PublishResponse publish(Long draftId, String actor) {
        PropertyDraftEntity draft = draftService.getById(draftId);
        PublishMapper.NormalizedPublishData normalized = publishMapper.toNormalized(draft.getWizardData());
        publishValidator.validate(normalized.root());

        String propertyId;
        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            propertyId = republishExistingProperty(normalized, draft);
        } else {
            propertyId = publishNewProperty(normalized, actor);
        }

        inventorySyncService.requestSyncAfterCommit(propertyId);

        draftService.markPublished(draft, propertyId, actor);
        return new PublishResponse(draftId, propertyId, DraftStatus.PUBLISHED.name());
    }

    private String publishNewProperty(PublishMapper.NormalizedPublishData normalized, String actor) {
        normalized.property().setCreatedBy(actor);
        PropertyEntity property = propertyRepository.save(normalized.property());
        String propertyId = property.getId();
        saveNormalizedData(normalized, propertyId);
        return propertyId;
    }

    private String republishExistingProperty(PublishMapper.NormalizedPublishData normalized, PropertyDraftEntity draft) {
        String propertyId = draft.getPublishedPropertyId();
        if (propertyId == null || propertyId.isBlank()) {
            throw new BadRequestException("Draft already published, but property id missing");
        }

        PropertyEntity existing = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));

        PropertyEntity updated = normalized.property();
        updated.setId(existing.getId());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        propertyRepository.save(updated);

        cleanupExistingPropertyImages(propertyId);
        clearNormalizedData(propertyId);
        saveNormalizedData(normalized, propertyId);
        return propertyId;
    }

    private void saveNormalizedData(PublishMapper.NormalizedPublishData normalized, String propertyId) {
        savePropertyOverview(normalized.propertyOverview(), propertyId);
        saveGuestServiceAmenities(normalized.guestServiceAmenities(), propertyId);
        saveNearbyLocationAccessibility(normalized.nearbyLocationAccessibility(), propertyId);
        savePropertyAreas(normalized.propertyAreas(), propertyId);
        saveRoomOutletTypes(normalized.roomOutletTypes(), propertyId);
        saveFloors(normalized.floors(), propertyId);
        saveFloorPropertyAreas(normalized.floorPropertyAreas(), propertyId);
        saveInventory(normalized.inventoryRooms(), propertyId);
        saveChartOfAccounts(normalized.chartOfAccounts(), propertyId);
        saveRevenueMappings(normalized.revenueMappings(), propertyId);
        savePaymentMethods(normalized.paymentMethods(), propertyId);
        saveTaxRules(normalized.taxRules(), propertyId);
    }

    private void clearNormalizedData(String propertyId) {
        taxRuleRepository.deleteByPropertyId(propertyId);
        paymentMethodRepository.deleteByPropertyId(propertyId);
        revenueMappingRepository.deleteByPropertyId(propertyId);
        chartOfAccountRepository.deleteByPropertyId(propertyId);
        inventoryRoomRepository.deleteByPropertyId(propertyId);
        floorPropertyAreaRepository.deleteByPropertyId(propertyId);
        floorConfigurationRepository.deleteByPropertyId(propertyId);
        roomOutletTypeRepository.deleteByPropertyId(propertyId);
        propertyAreaRepository.deleteByPropertyId(propertyId);
        nearbyLocationAccessibilityRepository.deleteByPropertyId(propertyId);
        guestServiceAmenityRepository.deleteByPropertyId(propertyId);
        propertyOverviewRepository.deleteByPropertyId(propertyId);

        taxRuleRepository.flush();
        paymentMethodRepository.flush();
        revenueMappingRepository.flush();
        chartOfAccountRepository.flush();
        inventoryRoomRepository.flush();
        floorPropertyAreaRepository.flush();
        floorConfigurationRepository.flush();
        roomOutletTypeRepository.flush();
        propertyAreaRepository.flush();
        nearbyLocationAccessibilityRepository.flush();
        guestServiceAmenityRepository.flush();
        propertyOverviewRepository.flush();
    }

    private void cleanupExistingPropertyImages(String propertyId) {
        propertyOverviewRepository.findByPropertyId(propertyId)
            .ifPresent(overview -> deleteImageIfUploadUrl(overview.getPropertyHeroImage()));

        List<PropertyAreaEntity> propertyAreas = propertyAreaRepository.findAllByPropertyId(propertyId);
        if (propertyAreas != null) {
            for (PropertyAreaEntity propertyArea : propertyAreas) {
                deleteImagesFromCsv(propertyArea.getImagesCsv());
            }
        }

        List<RoomOutletTypeEntity> roomOutletTypes = roomOutletTypeRepository.findAllByPropertyId(propertyId);
        if (roomOutletTypes != null) {
            for (RoomOutletTypeEntity roomOutletType : roomOutletTypes) {
                deleteImagesFromCsv(roomOutletType.getImagesCsv());
            }
        }
    }

    private void deleteImagesFromCsv(String csvValues) {
        if (csvValues == null || csvValues.isBlank()) {
            return;
        }
        for (String raw : csvValues.split(",")) {
            deleteImageIfUploadUrl(raw);
        }
    }

    private void deleteImageIfUploadUrl(String rawValue) {
        if (rawValue == null) {
            return;
        }
        String value = rawValue.trim();
        if (value.startsWith("/uploads/")) {
            localImageStorageService.deleteByPublicUrl(value);
        }
    }

    private void savePropertyAreas(List<PropertyAreaEntity> entities, String propertyId) {
        for (PropertyAreaEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        propertyAreaRepository.saveAll(entities);
    }

    private void saveRoomOutletTypes(List<RoomOutletTypeEntity> entities, String propertyId) {
        for (RoomOutletTypeEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        roomOutletTypeRepository.saveAll(entities);
    }

    private void savePropertyOverview(PropertyOverviewEntity propertyOverview, String propertyId) {
        if (propertyOverview == null) {
            return;
        }
        propertyOverview.setPropertyId(propertyId);
        propertyOverviewRepository.save(propertyOverview);
    }

    private void saveGuestServiceAmenities(List<GuestServiceAmenityEntity> entities, String propertyId) {
        for (GuestServiceAmenityEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        guestServiceAmenityRepository.saveAll(entities);
    }

    private void saveNearbyLocationAccessibility(List<NearbyLocationAccessibilityEntity> entities, String propertyId) {
        for (NearbyLocationAccessibilityEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        nearbyLocationAccessibilityRepository.saveAll(entities);
    }

    private void saveFloors(List<FloorConfigurationEntity> entities, String propertyId) {
        for (FloorConfigurationEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        floorConfigurationRepository.saveAll(entities);
    }

    private void saveInventory(List<InventoryRoomEntity> entities, String propertyId) {
        for (InventoryRoomEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        inventoryRoomRepository.saveAll(entities);
    }

    private void saveFloorPropertyAreas(List<FloorPropertyAreaEntity> entities, String propertyId) {
        for (FloorPropertyAreaEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        floorPropertyAreaRepository.saveAll(entities);
    }

    private void saveChartOfAccounts(List<ChartOfAccountEntity> entities, String propertyId) {
        for (ChartOfAccountEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        chartOfAccountRepository.saveAll(entities);
    }

    private void saveRevenueMappings(List<RevenueMappingEntity> entities, String propertyId) {
        for (RevenueMappingEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        revenueMappingRepository.saveAll(entities);
    }

    private void savePaymentMethods(List<PaymentMethodEntity> entities, String propertyId) {
        for (PaymentMethodEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        paymentMethodRepository.saveAll(entities);
    }

    private void saveTaxRules(List<TaxRuleEntity> entities, String propertyId) {
        for (TaxRuleEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        taxRuleRepository.saveAll(entities);
    }
}


