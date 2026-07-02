package com.pms.property.domain.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.room.dto.InventoryRoomRequest;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

    @Test
    void shouldCreateInventoryRoom() {
        FloorConfigurationRepository floorConfigRepository = mock(FloorConfigurationRepository.class);
        PropertyAreaRepository propertyAreaRepository = mock(PropertyAreaRepository.class);
        FloorPropertyAreaRepository floorPropertyAreaRepository = mock(FloorPropertyAreaRepository.class);
        RoomOutletTypeRepository roomOutletTypeRepository = mock(RoomOutletTypeRepository.class);
        InventoryRoomRepository inventoryRoomRepository = mock(InventoryRoomRepository.class);
        RoomService service = new RoomService(
            floorConfigRepository,
            propertyAreaRepository,
            floorPropertyAreaRepository,
            roomOutletTypeRepository,
            inventoryRoomRepository
        );

        when(inventoryRoomRepository.save(any(InventoryRoomEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createInventoryRoom("P-1", new InventoryRoomRequest("1", "Deluxe", "101"));

        assertEquals("P-1", response.propertyId());
        assertEquals("101", response.roomNumber());
    }

    @Test
    void shouldThrowWhenInventoryRoomNotFound() {
        FloorConfigurationRepository floorConfigRepository = mock(FloorConfigurationRepository.class);
        PropertyAreaRepository propertyAreaRepository = mock(PropertyAreaRepository.class);
        FloorPropertyAreaRepository floorPropertyAreaRepository = mock(FloorPropertyAreaRepository.class);
        RoomOutletTypeRepository roomOutletTypeRepository = mock(RoomOutletTypeRepository.class);
        InventoryRoomRepository inventoryRoomRepository = mock(InventoryRoomRepository.class);
        RoomService service = new RoomService(
            floorConfigRepository,
            propertyAreaRepository,
            floorPropertyAreaRepository,
            roomOutletTypeRepository,
            inventoryRoomRepository
        );

        when(inventoryRoomRepository.findByPropertyIdAndId("P-1", 33L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getInventoryRoomById("P-1", 33L));
    }

    @Test
    void shouldListRoomOutletTypesByPropertyId() {
        FloorConfigurationRepository floorConfigRepository = mock(FloorConfigurationRepository.class);
        PropertyAreaRepository propertyAreaRepository = mock(PropertyAreaRepository.class);
        FloorPropertyAreaRepository floorPropertyAreaRepository = mock(FloorPropertyAreaRepository.class);
        RoomOutletTypeRepository roomOutletTypeRepository = mock(RoomOutletTypeRepository.class);
        InventoryRoomRepository inventoryRoomRepository = mock(InventoryRoomRepository.class);
        RoomService service = new RoomService(
            floorConfigRepository,
            propertyAreaRepository,
            floorPropertyAreaRepository,
            roomOutletTypeRepository,
            inventoryRoomRepository
        );

        RoomOutletTypeEntity entity = new RoomOutletTypeEntity();
        entity.setId(8L);
        entity.setPropertyId("P-1");
        entity.setRoomName("Deluxe");
        entity.setRoomCode("DLX");
        entity.setQuantity(2);
        entity.setAvailableForSell(true);
        entity.setMaximumGuestOccupancy(2);
        entity.setDescription("Room desc");
        entity.setAmenitiesCsv("TV,WiFi");
        entity.setImagesCsv("img1,img2");

        when(roomOutletTypeRepository.findAllByPropertyId("P-1")).thenReturn(List.of(entity));

        var response = service.listRoomOutletTypesByPropertyId("P-1");

        assertEquals(1, response.size());
        assertEquals("Deluxe", response.get(0).roomName());
        assertEquals("DLX", response.get(0).roomCode());
        assertEquals("P-1", response.get(0).propertyId());
    }
}

