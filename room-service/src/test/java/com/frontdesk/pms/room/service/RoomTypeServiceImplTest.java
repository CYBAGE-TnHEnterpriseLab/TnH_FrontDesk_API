package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomTypeRequestDTO;
import com.frontdesk.pms.room.dto.RoomTypeResponseDTO;
import com.frontdesk.pms.room.entity.RoomType;
import com.frontdesk.pms.room.exception.BadRequestException;
import com.frontdesk.pms.room.exception.RoomTypeNotFoundException;
import com.frontdesk.pms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository repository;

    @Mock
    private PropertyValidationService propertyValidationService;

    @InjectMocks
    private RoomTypeServiceImpl service;

    @Test
    void createMasterRoomTypeSaves() {
        UUID propertyId = UUID.randomUUID();
        RoomTypeRequestDTO request = request(propertyId, "Deluxe", true, null);
        when(repository.findByNameAndPropertyId("Deluxe", propertyId)).thenReturn(Optional.empty());
        when(repository.save(any(RoomType.class))).thenAnswer(invocation -> {
            RoomType roomType = invocation.getArgument(0);
            roomType.setId(1L);
            return roomType;
        });

        RoomTypeResponseDTO response = service.createRoomType(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getIsMaster()).isTrue();
        verify(propertyValidationService).assertPropertyExists(propertyId);
    }

    @Test
    void createRoomTypeRejectsDuplicateNameForProperty() {
        UUID propertyId = UUID.randomUUID();
        RoomTypeRequestDTO request = request(propertyId, "Deluxe", true, null);
        when(repository.findByNameAndPropertyId("Deluxe", propertyId))
                .thenReturn(Optional.of(roomType(1L, propertyId, "Deluxe", true, null)));

        assertThatThrownBy(() -> service.createRoomType(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room type already exists");

        verify(repository, never()).save(any(RoomType.class));
    }

    @Test
    void createNonMasterRequiresMasterRoomType() {
        UUID propertyId = UUID.randomUUID();
        RoomTypeRequestDTO request = request(propertyId, "King", false, null);
        when(repository.findByNameAndPropertyId("King", propertyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRoomType(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Non-master must have masterRoomTypeId");
    }

    @Test
    void createNonMasterRejectsMasterFromDifferentProperty() {
        UUID propertyId = UUID.randomUUID();
        UUID otherPropertyId = UUID.randomUUID();
        RoomTypeRequestDTO request = request(propertyId, "King", false, 10L);
        when(repository.findByNameAndPropertyId("King", propertyId)).thenReturn(Optional.empty());
        when(repository.findById(10L)).thenReturn(Optional.of(roomType(10L, otherPropertyId, "Master", true, null)));

        assertThatThrownBy(() -> service.createRoomType(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Assigned master room type must belong to the same property");
    }

    @Test
    void updateRoomTypeRejectsNameUsedByAnotherRoomType() {
        UUID propertyId = UUID.randomUUID();
        RoomType existing = roomType(1L, propertyId, "Old", true, null);
        RoomType duplicate = roomType(2L, propertyId, "Deluxe", true, null);
        RoomTypeRequestDTO request = request(propertyId, "Deluxe", true, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndPropertyId("Deluxe", propertyId)).thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> service.updateRoomType(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room type name already exists");
    }

    @Test
    void getRoomTypeByIdThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRoomTypeById(1L))
                .isInstanceOf(RoomTypeNotFoundException.class);
    }

    private RoomTypeRequestDTO request(UUID propertyId, String name, Boolean isMaster, Long masterRoomTypeId) {
        RoomTypeRequestDTO request = new RoomTypeRequestDTO();
        request.setPropertyId(propertyId);
        request.setName(name);
        request.setIsMaster(isMaster);
        request.setMasterRoomTypeId(masterRoomTypeId);
        return request;
    }

    private RoomType roomType(Long id, UUID propertyId, String name, Boolean isMaster, Long masterRoomTypeId) {
        return RoomType.builder()
                .id(id)
                .propertyId(propertyId)
                .name(name)
                .isMaster(isMaster)
                .masterRoomTypeId(masterRoomTypeId)
                .build();
    }
}
