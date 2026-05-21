package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoom;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.MasterRoomRoomTypeMapping;
import com.frontdesk.pms.rate_management.mapper.MasterRoomMapper;
import com.frontdesk.pms.rate_management.repository.MasterRoomPricingRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomRepository;
import com.frontdesk.pms.rate_management.repository.MasterRoomRoomTypeMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterRoomServiceTest {

    @Mock
    private MasterRoomRepository masterRoomRepository;

    @Mock
    private MasterRoomPricingRepository masterRoomPricingRepository;

    @Mock
    private MasterRoomRoomTypeMappingRepository mappingRepository;

    @Mock
    private MasterRoomMapper masterRoomMapper;

    @InjectMocks
    private MasterRoomService masterRoomService;

    @Test
    void updateInheritedPricingForMasterRoom_shouldCreateMissingAndUpdateExistingInheritedPricing() {
        Long masterRoomId = 10L;
        Long roomTypeA = 101L;
        Long roomTypeB = 102L;

        MasterRoom masterRoom = new MasterRoom();
        masterRoom.setId(masterRoomId);

        MasterRoomPricing masterPricing = new MasterRoomPricing();
        masterPricing.setId(1000L);
        masterPricing.setMasterRoom(masterRoom);
        masterPricing.setOccupancyType("2P");
        masterPricing.setPrice(2500.0);

        MasterRoomPricing inheritedForA = new MasterRoomPricing();
        inheritedForA.setId(2000L);
        inheritedForA.setRoomTypeId(roomTypeA);
        inheritedForA.setInherited(true);
        inheritedForA.setParentPricingId(masterPricing.getId());
        inheritedForA.setOccupancyType("2P");
        inheritedForA.setPrice(1900.0);

        MasterRoomRoomTypeMapping mappingA = new MasterRoomRoomTypeMapping();
        mappingA.setRoomTypeId(roomTypeA);
        MasterRoomRoomTypeMapping mappingB = new MasterRoomRoomTypeMapping();
        mappingB.setRoomTypeId(roomTypeB);

        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(masterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(mappingA, mappingB));
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(masterPricing.getId()))
                .thenReturn(List.of(inheritedForA));
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeB, "2P"))
                .thenReturn(Optional.empty());

        masterRoomService.updateInheritedPricingForMasterRoom(masterRoomId);

        assertEquals(2500.0, inheritedForA.getPrice());
        verify(masterRoomPricingRepository, times(2)).save(any(MasterRoomPricing.class));

        ArgumentCaptor<MasterRoomPricing> captor = ArgumentCaptor.forClass(MasterRoomPricing.class);
        verify(masterRoomPricingRepository, times(2)).save(captor.capture());

        MasterRoomPricing created = captor.getAllValues().stream()
                .filter(p -> p.getId() == null)
                .findFirst()
                .orElse(null);

        assertNotNull(created);
        assertEquals(roomTypeB, created.getRoomTypeId());
        assertEquals("2P", created.getOccupancyType());
        assertEquals(2500.0, created.getPrice());
        assertEquals(true, created.getInherited());
        assertEquals(masterPricing.getId(), created.getParentPricingId());
    }

    @Test
    void updateInheritedPricingForMasterRoom_shouldNotOverrideManualRoomTypePricing() {
        Long masterRoomId = 11L;
        Long roomTypeId = 201L;

        MasterRoom masterRoom = new MasterRoom();
        masterRoom.setId(masterRoomId);

        MasterRoomPricing masterPricing = new MasterRoomPricing();
        masterPricing.setId(3000L);
        masterPricing.setMasterRoom(masterRoom);
        masterPricing.setOccupancyType("1P");
        masterPricing.setPrice(3000.0);

        MasterRoomRoomTypeMapping mapping = new MasterRoomRoomTypeMapping();
        mapping.setRoomTypeId(roomTypeId);

        MasterRoomPricing manualChildPricing = new MasterRoomPricing();
        manualChildPricing.setId(4000L);
        manualChildPricing.setRoomTypeId(roomTypeId);
        manualChildPricing.setOccupancyType("1P");
        manualChildPricing.setPrice(2800.0);
        manualChildPricing.setInherited(false);

        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(masterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(mapping));
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(masterPricing.getId()))
                .thenReturn(List.of());
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeId, "1P"))
                .thenReturn(Optional.of(manualChildPricing));

        masterRoomService.updateInheritedPricingForMasterRoom(masterRoomId);

        verify(masterRoomPricingRepository, never()).save(eq(manualChildPricing));
    }

    @Test
    void addOrUpdatePricing_shouldUpdateExistingMasterPricingForOccupancy() {
        Long masterRoomId = 12L;

        MasterRoom masterRoom = new MasterRoom();
        masterRoom.setId(masterRoomId);

        MasterRoomPricing existingMasterPricing = new MasterRoomPricing();
        existingMasterPricing.setId(5000L);
        existingMasterPricing.setMasterRoom(masterRoom);
        existingMasterPricing.setOccupancyType("3P");
        existingMasterPricing.setPrice(3200.0);

        MasterRoomPricingRequestDTO requestDTO = new MasterRoomPricingRequestDTO();
        requestDTO.setOccupancyType("3P");
        requestDTO.setPrice(3500.0);

        when(masterRoomRepository.findById(masterRoomId)).thenReturn(Optional.of(masterRoom));
        when(masterRoomPricingRepository.findByMasterRoomIdAndOccupancyType(masterRoomId, "3P"))
                .thenReturn(Optional.of(existingMasterPricing));
        when(masterRoomPricingRepository.save(existingMasterPricing)).thenReturn(existingMasterPricing);
        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(existingMasterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of());
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(existingMasterPricing.getId()))
                .thenReturn(List.of());

        masterRoomService.addOrUpdatePricing(masterRoomId, requestDTO);

        assertEquals(3500.0, existingMasterPricing.getPrice());
        verify(masterRoomPricingRepository, times(1)).save(existingMasterPricing);
    }
}
