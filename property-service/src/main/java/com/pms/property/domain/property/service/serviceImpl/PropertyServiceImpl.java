package com.pms.property.domain.property.service.serviceImpl;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.common.exception.NotFoundException;
import com.pms.property.common.exception.PropertyDeletionException;
import com.pms.property.domain.config.InventoryClient;
import com.pms.property.domain.config.HousekeepingClient;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import com.pms.property.domain.property.dto.PropertyResponse;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.property.mapper.PropertyMapper;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.domain.property.service.PropertyService;
import com.pms.property.domain.room.entity.PropertyAreaEntity;
import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.repository.PropertyDraftRepository;
import com.pms.property.draft.service.DraftService;
import com.pms.property.upload.service.LocalImageStorageService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyServiceImpl implements PropertyService {

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
    private final PropertyDraftRepository propertyDraftRepository;
    private final LocalImageStorageService localImageStorageService;
    private final DraftService draftService;
    private final InventoryClient inventoryClient;
    private final HousekeepingClient housekeepingClient;

    public PropertyServiceImpl(
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
            PropertyDraftRepository propertyDraftRepository,
            LocalImageStorageService localImageStorageService,
            DraftService draftService,
            InventoryClient inventoryClient,
            HousekeepingClient housekeepingClient
    ) {
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
        this.propertyDraftRepository = propertyDraftRepository;
        this.localImageStorageService = localImageStorageService;
        this.draftService = draftService;
        this.inventoryClient = inventoryClient;
        this.housekeepingClient = housekeepingClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getById(String propertyId) {
        return propertyRepository.findById(propertyId)
            .map(PropertyMapper::toResponse)
            .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> listByCreator(UUID creator) {
        return propertyRepository.findByCreatedBy(creator)
            .stream()
            .map(PropertyMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOwnedProperty(String propertyId, UUID actor) {
        PropertyEntity property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
        if (!actor.equals(property.getCreatedBy())) {
            throw new BadRequestException("Property does not belong to the current user");
        }

        LocalDate businessDate = LocalDate.now();
        // Check for active reservations before deleting anything
        boolean hasActiveReservations =
                inventoryClient.hasActiveReservations(
                        propertyId,
                        businessDate
                );


        System.out.println("hasActiveReservations = " + hasActiveReservations);

        if (hasActiveReservations) {
            throw new PropertyDeletionException(
                    "Property deletion cannot be done as property has active reservations for upcoming days"
            );
        }

        cleanupDraftImages(propertyId);
        cleanupNormalizedPropertyImages(propertyId);

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

        inventoryClient.deletePropertyData(propertyId);
        housekeepingClient.deletePropertyData(propertyId);

        propertyDraftRepository.deleteByPublishedPropertyId(propertyId);
        propertyRepository.delete(property);
    }

    private void cleanupDraftImages(String propertyId) {
        List<PropertyDraftEntity> linkedDrafts = propertyDraftRepository.findByPublishedPropertyId(propertyId);
        if (linkedDrafts == null) {
            return;
        }
        for (PropertyDraftEntity linkedDraft : linkedDrafts) {
            draftService.deleteImagesFromWizardData(linkedDraft.getWizardData());
        }
    }

    private void cleanupNormalizedPropertyImages(String propertyId) {
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
}


