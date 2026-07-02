package com.pms.property.domain.room.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.room.dto.InventoryRoomResponse;
import com.pms.property.domain.room.dto.RoomOutletTypeResponse;
import com.pms.property.domain.room.dto.RoomSummaryResponse;
import com.pms.property.domain.room.service.RoomService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomControllerTest {

    @Test
    void shouldReturnSummaryPayload() {
        RoomService service = mock(RoomService.class);
        RoomController controller = new RoomController(service);
        when(service.getSummaryByPropertyId("P-1")).thenReturn(new RoomSummaryResponse("P-1", 1, 1, 1, 1, 1));

        var response = controller.getSummary("P-1").getBody();

        assertEquals("P-1", response.data().propertyId());
    }

    @Test
    void shouldReturnInventoryRoomsPayload() {
        RoomService service = mock(RoomService.class);
        RoomController controller = new RoomController(service);
        when(service.listInventoryRoomsByPropertyId("P-1"))
            .thenReturn(List.of(new InventoryRoomResponse(1L, "P-1", "1", "Deluxe", "101")));

        var response = controller.listInventoryRooms("P-1").getBody();

        assertEquals(1, response.data().size());
    }

    @Test
    void shouldReturnRoomOutletTypesPayload() {
        RoomService service = mock(RoomService.class);
        RoomController controller = new RoomController(service);
        when(service.listRoomOutletTypesByPropertyId("P-1"))
            .thenReturn(List.of(new RoomOutletTypeResponse(1L, "P-1", "Deluxe", 2, true, 2, "Desc", "A1,A2", "I1,I2")));

        var response = controller.listRoomOutletTypes("P-1").getBody();

        assertEquals(1, response.data().size());
        assertEquals("Deluxe", response.data().get(0).roomName());
    }
}

