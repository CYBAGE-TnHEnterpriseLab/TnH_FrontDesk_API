package com.pms.property.domain.property.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.BadRequestException;
import com.pms.property.domain.content.repository.GuestServiceAmenityRepository;
import com.pms.property.domain.content.repository.NearbyLocationAccessibilityRepository;
import com.pms.property.domain.content.repository.PropertyOverviewRepository;
import com.pms.property.domain.finance.repository.ChartOfAccountRepository;
import com.pms.property.domain.finance.repository.RevenueMappingRepository;
import com.pms.property.domain.payment.repository.PaymentMethodRepository;
import com.pms.property.domain.property.entity.PropertyEntity;
import com.pms.property.domain.property.repository.PropertyRepository;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import com.pms.property.domain.tax.repository.TaxRuleRepository;
import com.pms.property.draft.repository.PropertyDraftRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PropertyServiceTest {

    @Test
    void shouldDeleteOwnedPropertyWithDependencies() {
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
        PropertyDraftRepository propertyDraftRepository = mock(PropertyDraftRepository.class);

        PropertyService service = new PropertyService(
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
            propertyDraftRepository
        );

        PropertyEntity property = new PropertyEntity();
        property.setId("P-200");
        property.setCreatedBy("owner");
        when(propertyRepository.findById("P-200")).thenReturn(Optional.of(property));

        service.deleteOwnedProperty("P-200", "owner");

        verify(taxRuleRepository).deleteByPropertyId("P-200");
        verify(paymentMethodRepository).deleteByPropertyId("P-200");
        verify(revenueMappingRepository).deleteByPropertyId("P-200");
        verify(chartOfAccountRepository).deleteByPropertyId("P-200");
        verify(inventoryRoomRepository).deleteByPropertyId("P-200");
        verify(floorPropertyAreaRepository).deleteByPropertyId("P-200");
        verify(floorConfigurationRepository).deleteByPropertyId("P-200");
        verify(roomOutletTypeRepository).deleteByPropertyId("P-200");
        verify(propertyAreaRepository).deleteByPropertyId("P-200");
        verify(nearbyLocationAccessibilityRepository).deleteByPropertyId("P-200");
        verify(guestServiceAmenityRepository).deleteByPropertyId("P-200");
        verify(propertyOverviewRepository).deleteByPropertyId("P-200");
        verify(propertyDraftRepository).deleteByPublishedPropertyId("P-200");
        verify(propertyRepository).delete(property);
    }

    @Test
    void shouldRejectDeleteWhenPropertyNotOwnedByActor() {
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
        PropertyDraftRepository propertyDraftRepository = mock(PropertyDraftRepository.class);

        PropertyService service = new PropertyService(
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
            propertyDraftRepository
        );

        PropertyEntity property = new PropertyEntity();
        property.setId("P-201");
        property.setCreatedBy("another-user");
        when(propertyRepository.findById("P-201")).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () -> service.deleteOwnedProperty("P-201", "owner"));

        verify(propertyRepository, never()).delete(property);
        verify(propertyDraftRepository, never()).deleteByPublishedPropertyId("P-201");
    }
}

