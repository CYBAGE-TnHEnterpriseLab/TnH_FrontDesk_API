package com.pms.property.domain.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.common.exception.NotFoundException;
import com.pms.property.domain.room.dto.InventoryRoomRequest;
import com.pms.property.domain.room.entity.InventoryRoomEntity;
import com.pms.property.domain.room.repository.FloorConfigurationRepository;
import com.pms.property.domain.room.repository.FloorPropertyAreaRepository;
import com.pms.property.domain.room.repository.InventoryRoomRepository;
import com.pms.property.domain.room.repository.PropertyAreaRepository;
import com.pms.property.domain.room.repository.RoomOutletTypeRepository;
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
}

