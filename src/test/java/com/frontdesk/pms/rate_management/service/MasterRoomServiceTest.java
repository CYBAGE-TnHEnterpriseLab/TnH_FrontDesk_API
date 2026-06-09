package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.MasterRoomPricingRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRequestDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomResponseDTO;
import com.frontdesk.pms.rate_management.dto.MasterRoomRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.dto.PropertyRoomTypeMappingResponseDTO;
import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.frontdesk.pms.rate_management.entity.MasterRoom;
import com.frontdesk.pms.rate_management.entity.MasterRoomPricing;
import com.frontdesk.pms.rate_management.entity.MasterRoomRoomTypeMapping;
import com.frontdesk.pms.rate_management.exception.MasterRoomNotFoundException;
import com.frontdesk.pms.rate_management.exception.PropertyNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        @Mock
        private PropertyServiceClient propertyServiceClient;

        @Mock
        private RoomServiceClient roomServiceClient;

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
        masterPricing.setOccupancyType("2 Guest");
        masterPricing.setPrice(2500.0);

        MasterRoomPricing inheritedForA = new MasterRoomPricing();
        inheritedForA.setId(2000L);
        inheritedForA.setRoomTypeId(roomTypeA);
        inheritedForA.setInherited(true);
        inheritedForA.setParentPricingId(masterPricing.getId());
        inheritedForA.setOccupancyType("2 Guest");
        inheritedForA.setPrice(1900.0);

        MasterRoomRoomTypeMapping mappingA = new MasterRoomRoomTypeMapping();
        mappingA.setRoomTypeId(roomTypeA);
        MasterRoomRoomTypeMapping mappingB = new MasterRoomRoomTypeMapping();
        mappingB.setRoomTypeId(roomTypeB);

        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(masterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(mappingA, mappingB));
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(masterPricing.getId()))
                .thenReturn(List.of(inheritedForA));
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeB, "2 Guest"))
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
        assertEquals("2 Guest", created.getOccupancyType());
        assertEquals(2500.0, created.getPrice());
        assertEquals(true, created.getInherited());
        assertEquals(masterPricing.getId(), created.getParentPricingId());
    }

        @Test
        void upsertRoomTypeMapping_shouldCreateWhenNotExists() {
                String propertyId = "11111111-1111-1111-1111-111111111111";
                Long roomTypeId = 601L;
                Long masterRoomId = 6L;

                MasterRoom masterRoom = new MasterRoom();
                masterRoom.setId(masterRoomId);
                masterRoom.setPropertyId(propertyId);

                MasterRoomPricing masterPricing = new MasterRoomPricing();
                masterPricing.setId(6001L);
                masterPricing.setMasterRoom(masterRoom);
                masterPricing.setOccupancyType("2 Guest");
                masterPricing.setPrice(2600.0);

                MasterRoomRoomTypeMapping savedMapping = new MasterRoomRoomTypeMapping();
                savedMapping.setId(61L);
                savedMapping.setMasterRoom(masterRoom);
                savedMapping.setRoomTypeId(roomTypeId);

                MasterRoomRoomTypeMappingResponseDTO responseDTO = new MasterRoomRoomTypeMappingResponseDTO();
                responseDTO.setId(61L);
                responseDTO.setRoomTypeId(roomTypeId);

                when(masterRoomRepository.findById(masterRoomId)).thenReturn(Optional.of(masterRoom));
                when(mappingRepository.findByMasterRoomPropertyIdAndRoomTypeId(propertyId, roomTypeId)).thenReturn(Optional.empty());
                when(mappingRepository.save(any(MasterRoomRoomTypeMapping.class))).thenReturn(savedMapping);
                when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(masterPricing));
                when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeId, "2 Guest")).thenReturn(Optional.empty());
                when(masterRoomMapper.toMappingResponseDTO(savedMapping)).thenReturn(responseDTO);

                MasterRoomRoomTypeMappingResponseDTO result = masterRoomService.upsertRoomTypeMapping(propertyId, roomTypeId, masterRoomId);

                assertEquals(61L, result.getId());
                assertEquals(roomTypeId, result.getRoomTypeId());
                verify(mappingRepository, times(1)).save(any(MasterRoomRoomTypeMapping.class));
                verify(masterRoomPricingRepository, times(1)).save(any(MasterRoomPricing.class));
        }

        @Test
        void upsertRoomTypeMapping_shouldUpdateWhenExists() {
                String propertyId = "11111111-1111-1111-1111-111111111111";
                Long roomTypeId = 701L;
                Long oldMasterRoomId = 7L;
                Long newMasterRoomId = 8L;

                MasterRoom oldMasterRoom = new MasterRoom();
                oldMasterRoom.setId(oldMasterRoomId);
                oldMasterRoom.setPropertyId(propertyId);

                MasterRoom newMasterRoom = new MasterRoom();
                newMasterRoom.setId(newMasterRoomId);
                newMasterRoom.setPropertyId(propertyId);

                MasterRoomRoomTypeMapping existingMapping = new MasterRoomRoomTypeMapping();
                existingMapping.setId(71L);
                existingMapping.setMasterRoom(oldMasterRoom);
                existingMapping.setRoomTypeId(roomTypeId);

                MasterRoomPricing newMasterPricing = new MasterRoomPricing();
                newMasterPricing.setId(8001L);
                newMasterPricing.setMasterRoom(newMasterRoom);
                newMasterPricing.setOccupancyType("2 Guest");
                newMasterPricing.setPrice(2800.0);

                MasterRoomRoomTypeMappingResponseDTO responseDTO = new MasterRoomRoomTypeMappingResponseDTO();
                responseDTO.setId(71L);
                responseDTO.setRoomTypeId(roomTypeId);

                when(masterRoomRepository.findById(newMasterRoomId)).thenReturn(Optional.of(newMasterRoom));
                when(mappingRepository.findByMasterRoomPropertyIdAndRoomTypeId(propertyId, roomTypeId)).thenReturn(Optional.of(existingMapping));
                when(mappingRepository.save(existingMapping)).thenReturn(existingMapping);
                when(masterRoomPricingRepository.findByMasterRoomId(newMasterRoomId)).thenReturn(List.of(newMasterPricing));
                when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeId, "2 Guest")).thenReturn(Optional.empty());
                when(masterRoomMapper.toMappingResponseDTO(existingMapping)).thenReturn(responseDTO);

                MasterRoomRoomTypeMappingResponseDTO result = masterRoomService.upsertRoomTypeMapping(propertyId, roomTypeId, newMasterRoomId);

                assertEquals(71L, result.getId());
                assertEquals(newMasterRoomId, existingMapping.getMasterRoom().getId());
                verify(mappingRepository, times(1)).save(existingMapping);
                verify(masterRoomPricingRepository, times(1)).save(any(MasterRoomPricing.class));
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
        masterPricing.setOccupancyType("1 Guest");
        masterPricing.setPrice(3000.0);

        MasterRoomRoomTypeMapping mapping = new MasterRoomRoomTypeMapping();
        mapping.setRoomTypeId(roomTypeId);

        MasterRoomPricing manualChildPricing = new MasterRoomPricing();
        manualChildPricing.setId(4000L);
        manualChildPricing.setRoomTypeId(roomTypeId);
        manualChildPricing.setOccupancyType("1 Guest");
        manualChildPricing.setPrice(2800.0);
        manualChildPricing.setInherited(false);

        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(masterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(mapping));
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(masterPricing.getId()))
                .thenReturn(List.of());
        when(masterRoomPricingRepository.findByRoomTypeIdAndOccupancyType(roomTypeId, "1 Guest"))
                .thenReturn(Optional.of(manualChildPricing));

        masterRoomService.updateInheritedPricingForMasterRoom(masterRoomId);

        verify(masterRoomPricingRepository, never()).save(eq(manualChildPricing));
    }

        @Test
        void getMappingsByPropertyId_shouldReturnMappingsWithInheritedRates() {
                String propertyId = "11111111-1111-1111-1111-111111111111";

                MasterRoom masterRoom = new MasterRoom();
                masterRoom.setId(5L);
                masterRoom.setName("Standard Master");
                masterRoom.setPropertyId(propertyId);

                MasterRoomRoomTypeMapping mapping = new MasterRoomRoomTypeMapping();
                mapping.setId(50L);
                mapping.setMasterRoom(masterRoom);
                mapping.setRoomTypeId(501L);

                MasterRoomPricing inheritedPricing = new MasterRoomPricing();
                inheritedPricing.setRoomTypeId(501L);
                inheritedPricing.setOccupancyType("2 Guest");
                inheritedPricing.setPrice(2500.0);

                com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO pricingResponse =
                                new com.frontdesk.pms.rate_management.dto.MasterRoomPricingResponseDTO();
                pricingResponse.setOccupancyType("2 Guest");
                pricingResponse.setPrice(2500.0);

                RoomDTO roomType = new RoomDTO();
                roomType.setId(501L);
                roomType.setName("Standard King");
                roomType.setActive(true);

                when(roomServiceClient.getRoomTypesByProperty(propertyId)).thenReturn(new RoomDTO[]{roomType});
                when(mappingRepository.findByMasterRoomPropertyId(propertyId)).thenReturn(List.of(mapping));
                when(masterRoomPricingRepository.findByRoomTypeId(501L)).thenReturn(List.of(inheritedPricing));
                when(masterRoomMapper.toPricingResponseDTO(inheritedPricing)).thenReturn(pricingResponse);

                List<PropertyRoomTypeMappingResponseDTO> result = masterRoomService.getMappingsByPropertyId(propertyId);

                assertEquals(1, result.size());
                assertEquals(50L, result.get(0).getMappingId());
                assertEquals(501L, result.get(0).getRoomTypeId());
                assertEquals("Standard King", result.get(0).getRoomTypeName());
                assertEquals(true, result.get(0).isMapped());
                assertEquals(5L, result.get(0).getMasterRoomId());
                assertEquals("Standard Master", result.get(0).getMasterRoomName());
                assertEquals(1, result.get(0).getInheritedRates().size());
                assertEquals("2 Guest", result.get(0).getInheritedRates().get(0).getOccupancyType());
        }

        @Test
        void getMappingsByPropertyId_shouldFallbackToPersistedMappingsWhenRoomServiceReturnsEmpty() {
                String propertyId = "11111111-1111-1111-1111-111111111111";

                MasterRoom masterRoom = new MasterRoom();
                masterRoom.setId(7L);
                masterRoom.setName("Fallback Master");
                masterRoom.setPropertyId(propertyId);

                MasterRoomRoomTypeMapping mapping = new MasterRoomRoomTypeMapping();
                mapping.setId(70L);
                mapping.setMasterRoom(masterRoom);
                mapping.setRoomTypeId(701L);

                when(roomServiceClient.getRoomTypesByProperty(propertyId)).thenReturn(new RoomDTO[]{});
                when(mappingRepository.findByMasterRoomPropertyId(propertyId)).thenReturn(List.of(mapping));
                when(masterRoomPricingRepository.findByRoomTypeId(701L)).thenReturn(List.of());

                List<PropertyRoomTypeMappingResponseDTO> result = masterRoomService.getMappingsByPropertyId(propertyId);

                assertEquals(1, result.size());
                assertEquals(70L, result.get(0).getMappingId());
                assertEquals(701L, result.get(0).getRoomTypeId());
                assertEquals(true, result.get(0).isMapped());
                assertEquals(7L, result.get(0).getMasterRoomId());
                assertEquals("Fallback Master", result.get(0).getMasterRoomName());
        }

    @Test
    void addOrUpdatePricing_shouldUpdateExistingMasterPricingForOccupancy() {
        Long masterRoomId = 12L;
                String propertyId = "11111111-1111-1111-1111-111111111111";

        MasterRoom masterRoom = new MasterRoom();
        masterRoom.setId(masterRoomId);
                masterRoom.setPropertyId(propertyId);

        MasterRoomPricing existingMasterPricing = new MasterRoomPricing();
        existingMasterPricing.setId(5000L);
        existingMasterPricing.setMasterRoom(masterRoom);
        existingMasterPricing.setOccupancyType("3 Guest");
        existingMasterPricing.setPrice(3200.0);

        MasterRoomPricingRequestDTO requestDTO = new MasterRoomPricingRequestDTO();
        requestDTO.setOccupancyType("3 Guest");
        requestDTO.setPrice(3500.0);

        when(masterRoomRepository.findById(masterRoomId)).thenReturn(Optional.of(masterRoom));
        when(masterRoomPricingRepository.findByMasterRoomIdAndOccupancyType(masterRoomId, "3 Guest"))
                .thenReturn(Optional.of(existingMasterPricing));
        when(masterRoomPricingRepository.save(existingMasterPricing)).thenReturn(existingMasterPricing);
        when(masterRoomPricingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of(existingMasterPricing));
        when(mappingRepository.findByMasterRoomId(masterRoomId)).thenReturn(List.of());
        when(masterRoomPricingRepository.findByInheritedTrueAndParentPricingId(existingMasterPricing.getId()))
                .thenReturn(List.of());

        masterRoomService.addOrUpdatePricing(propertyId, masterRoomId, requestDTO);

        assertEquals(3500.0, existingMasterPricing.getPrice());
        verify(masterRoomPricingRepository, times(1)).save(existingMasterPricing);
    }

        @Test
        void addOrUpdatePricing_shouldThrowWhenMasterRoomDoesNotBelongToPathProperty() {
                Long masterRoomId = 13L;
                String propertyId = "11111111-1111-1111-1111-111111111111";

                MasterRoom masterRoom = new MasterRoom();
                masterRoom.setId(masterRoomId);
                masterRoom.setPropertyId("22222222-2222-2222-2222-222222222222");

                MasterRoomPricingRequestDTO requestDTO = new MasterRoomPricingRequestDTO();
                requestDTO.setOccupancyType("2 Guest");
                requestDTO.setPrice(3000.0);

                when(masterRoomRepository.findById(masterRoomId)).thenReturn(Optional.of(masterRoom));

                assertThrows(MasterRoomNotFoundException.class,
                                () -> masterRoomService.addOrUpdatePricing(propertyId, masterRoomId, requestDTO));
                verify(masterRoomPricingRepository, never()).save(any(MasterRoomPricing.class));
        }

        @Test
        void createMasterRoom_shouldThrowWhenPropertyDoesNotExist() {
                MasterRoomRequestDTO requestDTO = new MasterRoomRequestDTO();
                requestDTO.setName("Deluxe");

                when(propertyServiceClient.propertyExists("99999999-9999-9999-9999-999999999999")).thenReturn(false);

                assertThrows(PropertyNotFoundException.class, () -> masterRoomService.createMasterRoom("99999999-9999-9999-9999-999999999999", requestDTO));
                verify(masterRoomRepository, never()).save(any(MasterRoom.class));
        }

        @Test
        void createMasterRoom_shouldSaveWhenPropertyExists() {
                MasterRoomRequestDTO requestDTO = new MasterRoomRequestDTO();
                requestDTO.setName("Standard");

                MasterRoom entity = new MasterRoom();
                entity.setPropertyId("11111111-1111-1111-1111-111111111111");
                entity.setName("Standard");

                MasterRoom saved = new MasterRoom();
                saved.setId(1L);
                saved.setPropertyId("11111111-1111-1111-1111-111111111111");
                saved.setName("Standard");

                MasterRoomResponseDTO responseDTO = new MasterRoomResponseDTO();
                responseDTO.setId(1L);
                responseDTO.setPropertyId("11111111-1111-1111-1111-111111111111");
                responseDTO.setName("Standard");

                when(propertyServiceClient.propertyExists("11111111-1111-1111-1111-111111111111")).thenReturn(true);
                when(masterRoomMapper.toEntity(requestDTO)).thenReturn(entity);
                when(masterRoomRepository.save(entity)).thenReturn(saved);
                when(masterRoomMapper.toResponseDTO(saved)).thenReturn(responseDTO);

                MasterRoomResponseDTO result = masterRoomService.createMasterRoom("11111111-1111-1111-1111-111111111111", requestDTO);

                assertEquals(1L, result.getId());
                assertEquals("11111111-1111-1111-1111-111111111111", result.getPropertyId());
                verify(masterRoomRepository, times(1)).save(entity);
        }

        @Test
        void updateMasterRoom_shouldUpdateNameOnly() {
                MasterRoomRequestDTO requestDTO = new MasterRoomRequestDTO();
                requestDTO.setName("Executive Deluxe");

                MasterRoom existing = new MasterRoom();
                existing.setId(10L);
                existing.setPropertyId("11111111-1111-1111-1111-111111111111");
                existing.setName("Deluxe");

                MasterRoomResponseDTO responseDTO = new MasterRoomResponseDTO();
                responseDTO.setId(10L);
                responseDTO.setPropertyId("11111111-1111-1111-1111-111111111111");
                responseDTO.setName("Executive Deluxe");

                when(masterRoomRepository.findById(10L)).thenReturn(Optional.of(existing));
                when(masterRoomRepository.save(existing)).thenReturn(existing);
                when(masterRoomMapper.toResponseDTO(existing)).thenReturn(responseDTO);

                MasterRoomResponseDTO result = masterRoomService.updateMasterRoom("11111111-1111-1111-1111-111111111111", 10L, requestDTO);

                assertEquals("Executive Deluxe", result.getName());
                assertEquals("11111111-1111-1111-1111-111111111111", result.getPropertyId());
                verify(propertyServiceClient, never()).propertyExists(any(String.class));
        }

        @Test
        void updateMasterRoom_shouldThrowWhenMasterRoomDoesNotBelongToPathProperty() {
                MasterRoomRequestDTO requestDTO = new MasterRoomRequestDTO();
                requestDTO.setName("Suite Updated");

                MasterRoom existing = new MasterRoom();
                existing.setId(12L);
                existing.setPropertyId("22222222-2222-2222-2222-222222222222");
                existing.setName("Suite");

                when(masterRoomRepository.findById(12L)).thenReturn(Optional.of(existing));

                assertThrows(MasterRoomNotFoundException.class, () -> masterRoomService.updateMasterRoom("11111111-1111-1111-1111-111111111111", 12L, requestDTO));
                verify(masterRoomRepository, never()).save(any(MasterRoom.class));
        }
}

