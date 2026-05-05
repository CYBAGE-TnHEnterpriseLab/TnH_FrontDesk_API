package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.dto.RoomRequestDTO;
import com.frontdesk.pms.room.dto.RoomResponseDTO;
import com.frontdesk.pms.room.entity.Floor;
import com.frontdesk.pms.room.entity.Room;
import com.frontdesk.pms.room.entity.RoomType;
import com.frontdesk.pms.room.exception.BadRequestException;
import com.frontdesk.pms.room.exception.FloorNotFoundException;
import com.frontdesk.pms.room.exception.RoomNotFoundException;
import com.frontdesk.pms.room.exception.RoomTypeNotFoundException;
import com.frontdesk.pms.room.repository.FloorRepository;
import com.frontdesk.pms.room.repository.RoomRepository;
import com.frontdesk.pms.room.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository repository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private PropertyValidationService propertyValidationService;

    @InjectMocks
    private RoomServiceImpl service;

    @Test
    void createRoomsGeneratesSequentialRoomNumbers() {
        UUID propertyId = UUID.randomUUID();
        RoomRequestDTO request = request(propertyId, 5L, 9L, 2);
        when(floorRepository.findById(5L)).thenReturn(Optional.of(floor(5L, propertyId, 3)));
        when(roomTypeRepository.findById(9L)).thenReturn(Optional.of(roomType(9L, propertyId)));
        when(repository.findByFloorId(5L)).thenReturn(List.of(room(1L, "301", 5L, 9L, propertyId)));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Room> rooms = invocation.getArgument(0);
            rooms.get(0).setId(2L);
            rooms.get(1).setId(3L);
            return rooms;
        });

        List<RoomResponseDTO> response = service.createRooms(request);

        assertThat(response).extracting(RoomResponseDTO::getRoomNumber).containsExactly("302", "303");
        verify(propertyValidationService).assertPropertyExists(propertyId);

        ArgumentCaptor<List<Room>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void createRoomsRejectsFloorFromDifferentProperty() {
        UUID propertyId = UUID.randomUUID();
        RoomRequestDTO request = request(propertyId, 5L, 9L, 1);
        when(floorRepository.findById(5L)).thenReturn(Optional.of(floor(5L, UUID.randomUUID(), 3)));
        when(roomTypeRepository.findById(9L)).thenReturn(Optional.of(roomType(9L, propertyId)));

        assertThatThrownBy(() -> service.createRooms(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Floor does not belong to the requested property");
    }

    @Test
    void createRoomsThrowsWhenFloorMissing() {
        UUID propertyId = UUID.randomUUID();
        RoomRequestDTO request = request(propertyId, 5L, 9L, 1);
        when(floorRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRooms(request))
                .isInstanceOf(FloorNotFoundException.class);
    }

    @Test
    void updateRoomChangesRoomTypeWhenSameProperty() {
        UUID propertyId = UUID.randomUUID();
        Room room = room(1L, "301", 5L, 9L, propertyId);
        when(repository.findById(1L)).thenReturn(Optional.of(room));
        when(roomTypeRepository.findById(10L)).thenReturn(Optional.of(roomType(10L, propertyId)));
        when(repository.save(room)).thenReturn(room);

        RoomResponseDTO response = service.updateRoom(1L, 10L);

        assertThat(response.getRoomTypeId()).isEqualTo(10L);
        assertThat(room.getRoomTypeId()).isEqualTo(10L);
    }

    @Test
    void updateRoomRejectsRoomTypeFromDifferentProperty() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findById(1L)).thenReturn(Optional.of(room(1L, "301", 5L, 9L, propertyId)));
        when(roomTypeRepository.findById(10L)).thenReturn(Optional.of(roomType(10L, UUID.randomUUID())));

        assertThatThrownBy(() -> service.updateRoom(1L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("RoomType does not belong to the same property as Room");
    }

    @Test
    void updateRoomThrowsWhenRoomTypeMissing() {
        when(repository.findById(1L)).thenReturn(Optional.of(room(1L, "301", 5L, 9L, UUID.randomUUID())));
        when(roomTypeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoom(1L, 10L))
                .isInstanceOf(RoomTypeNotFoundException.class);
    }

    @Test
    void deleteRoomThrowsWhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteRoom(1L))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void getRoomsByPropertyValidatesAndMapsResults() {
        UUID propertyId = UUID.randomUUID();
        when(repository.findByPropertyId(propertyId)).thenReturn(List.of(room(1L, "301", 5L, 9L, propertyId)));

        List<RoomResponseDTO> response = service.getRoomsByProperty(propertyId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRoomNumber()).isEqualTo("301");
        verify(propertyValidationService).assertPropertyExists(propertyId);
    }

    private RoomRequestDTO request(UUID propertyId, Long floorId, Long roomTypeId, Integer numberOfRooms) {
        RoomRequestDTO request = new RoomRequestDTO();
        request.setPropertyId(propertyId);
        request.setFloorId(floorId);
        request.setRoomTypeId(roomTypeId);
        request.setNumberOfRooms(numberOfRooms);
        return request;
    }

    private Floor floor(Long id, UUID propertyId, Integer floorNumber) {
        return Floor.builder()
                .id(id)
                .propertyId(propertyId)
                .floorNumber(floorNumber)
                .name("Floor " + floorNumber)
                .build();
    }

    private RoomType roomType(Long id, UUID propertyId) {
        return RoomType.builder()
                .id(id)
                .propertyId(propertyId)
                .name("Deluxe")
                .isMaster(true)
                .build();
    }

    private Room room(Long id, String roomNumber, Long floorId, Long roomTypeId, UUID propertyId) {
        return Room.builder()
                .id(id)
                .roomNumber(roomNumber)
                .floorId(floorId)
                .roomTypeId(roomTypeId)
                .propertyId(propertyId)
                .build();
    }
}
