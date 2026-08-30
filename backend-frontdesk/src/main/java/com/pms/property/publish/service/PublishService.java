package com.pms.property.publish.service;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.content.AmenityEntity;
import com.pms.property.domain.content.AmenityRepository;
import com.pms.property.domain.content.SpecialRequestEntity;
import com.pms.property.domain.content.SpecialRequestRepository;
import com.pms.property.domain.finance.ChartOfAccountEntity;
import com.pms.property.domain.finance.ChartOfAccountRepository;
import com.pms.property.domain.finance.RevenueMappingEntity;
import com.pms.property.domain.finance.RevenueMappingRepository;
import com.pms.property.domain.payment.PaymentGatewayConfigEntity;
import com.pms.property.domain.payment.PaymentGatewayConfigRepository;
import com.pms.property.domain.payment.PaymentMethodEntity;
import com.pms.property.domain.payment.PaymentMethodRepository;
import com.pms.property.domain.property.PropertyEntity;
import com.pms.property.domain.property.PropertyRepository;
import com.pms.property.domain.room.FloorConfigurationEntity;
import com.pms.property.domain.room.FloorConfigurationRepository;
import com.pms.property.domain.room.InventoryRoomEntity;
import com.pms.property.domain.room.InventoryRoomRepository;
import com.pms.property.domain.room.RoomTypeEntity;
import com.pms.property.domain.room.RoomTypeRepository;
import com.pms.property.domain.tax.TaxRuleEntity;
import com.pms.property.domain.tax.TaxRuleRepository;
import com.pms.property.draft.entity.DraftStatus;
import com.pms.property.draft.entity.PropertyDraftEntity;
import com.pms.property.draft.service.DraftService;
import com.pms.property.publish.dto.PublishResponse;
import com.pms.property.publish.mapper.PublishMapper;
import com.pms.property.publish.validator.PublishValidator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishService {

    private final DraftService draftService;
    private final PropertyRepository propertyRepository;
    private final SpecialRequestRepository specialRequestRepository;
    private final AmenityRepository amenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final FloorConfigurationRepository floorConfigurationRepository;
    private final InventoryRoomRepository inventoryRoomRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final RevenueMappingRepository revenueMappingRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentGatewayConfigRepository paymentGatewayConfigRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final PublishMapper publishMapper;
    private final PublishValidator publishValidator;

    public PublishService(
        DraftService draftService,
        PropertyRepository propertyRepository,
        SpecialRequestRepository specialRequestRepository,
        AmenityRepository amenityRepository,
        RoomTypeRepository roomTypeRepository,
        FloorConfigurationRepository floorConfigurationRepository,
        InventoryRoomRepository inventoryRoomRepository,
        ChartOfAccountRepository chartOfAccountRepository,
        RevenueMappingRepository revenueMappingRepository,
        PaymentMethodRepository paymentMethodRepository,
        PaymentGatewayConfigRepository paymentGatewayConfigRepository,
        TaxRuleRepository taxRuleRepository,
        PublishMapper publishMapper,
        PublishValidator publishValidator
    ) {
        this.draftService = draftService;
        this.propertyRepository = propertyRepository;
        this.specialRequestRepository = specialRequestRepository;
        this.amenityRepository = amenityRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.floorConfigurationRepository = floorConfigurationRepository;
        this.inventoryRoomRepository = inventoryRoomRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.revenueMappingRepository = revenueMappingRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentGatewayConfigRepository = paymentGatewayConfigRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.publishMapper = publishMapper;
        this.publishValidator = publishValidator;
    }

    @Transactional
    public PublishResponse publish(Long draftId) {
        PropertyDraftEntity draft = draftService.getById(draftId);
        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            if (draft.getPublishedPropertyId() == null) {
                throw new BadRequestException("Draft already published, but property id missing");
            }
            return new PublishResponse(draftId, draft.getPublishedPropertyId(), DraftStatus.PUBLISHED.name());
        }

        PublishMapper.NormalizedPublishData normalized = publishMapper.toNormalized(draft.getWizardData());
        publishValidator.validate(normalized.root());

        PropertyEntity property = propertyRepository.save(normalized.property());
        Long propertyId = property.getId();

        saveSpecialRequests(normalized.specialRequests(), propertyId);
        saveAmenities(normalized.amenities(), propertyId);
        saveRoomTypes(normalized.roomTypes(), propertyId);
        saveFloors(normalized.floors(), propertyId);
        saveInventory(normalized.inventoryRooms(), propertyId);
        saveChartOfAccounts(normalized.chartOfAccounts(), propertyId);
        saveRevenueMappings(normalized.revenueMappings(), propertyId);
        savePaymentMethods(normalized.paymentMethods(), propertyId);

        PaymentGatewayConfigEntity gateway = normalized.gateway();
        gateway.setPropertyId(propertyId);
        paymentGatewayConfigRepository.save(gateway);

        saveTaxRules(normalized.taxRules(), propertyId);

        draftService.markPublished(draft, propertyId);
        return new PublishResponse(draftId, propertyId, DraftStatus.PUBLISHED.name());
    }

    private void saveSpecialRequests(List<SpecialRequestEntity> entities, Long propertyId) {
        for (SpecialRequestEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        specialRequestRepository.saveAll(entities);
    }

    private void saveAmenities(List<AmenityEntity> entities, Long propertyId) {
        for (AmenityEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        amenityRepository.saveAll(entities);
    }

    private void saveRoomTypes(List<RoomTypeEntity> entities, Long propertyId) {
        for (RoomTypeEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        roomTypeRepository.saveAll(entities);
    }

    private void saveFloors(List<FloorConfigurationEntity> entities, Long propertyId) {
        for (FloorConfigurationEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        floorConfigurationRepository.saveAll(entities);
    }

    private void saveInventory(List<InventoryRoomEntity> entities, Long propertyId) {
        for (InventoryRoomEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        inventoryRoomRepository.saveAll(entities);
    }

    private void saveChartOfAccounts(List<ChartOfAccountEntity> entities, Long propertyId) {
        for (ChartOfAccountEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        chartOfAccountRepository.saveAll(entities);
    }

    private void saveRevenueMappings(List<RevenueMappingEntity> entities, Long propertyId) {
        for (RevenueMappingEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        revenueMappingRepository.saveAll(entities);
    }

    private void savePaymentMethods(List<PaymentMethodEntity> entities, Long propertyId) {
        for (PaymentMethodEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        paymentMethodRepository.saveAll(entities);
    }

    private void saveTaxRules(List<TaxRuleEntity> entities, Long propertyId) {
        for (TaxRuleEntity entity : entities) {
            entity.setPropertyId(propertyId);
        }
        taxRuleRepository.saveAll(entities);
    }
}



