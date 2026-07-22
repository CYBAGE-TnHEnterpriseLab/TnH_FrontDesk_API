package com.pms.property.integration.inventory.service;

import com.pms.property.domain.room.entity.RoomOutletTypeEntity;
import com.pms.property.integration.inventory.dto.InventoryReconciliationRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySyncPayloadMapperTest {

    @Test
    void mapsSellableAndNonSellableRoomTypes() {
        InventorySyncPayloadMapper mapper = new InventorySyncPayloadMapper(30);

        RoomOutletTypeEntity sellable = new RoomOutletTypeEntity();
        sellable.setRoomName("Deluxe");
        sellable.setRoomCode("DLX");
        sellable.setQuantity(3);
        sellable.setAvailableForSell(true);

        RoomOutletTypeEntity hidden = new RoomOutletTypeEntity();
        hidden.setRoomName("Maintenance");
        hidden.setRoomCode("MNT");
        hidden.setQuantity(2);
        hidden.setAvailableForSell(false);

        InventoryReconciliationRequest request = mapper.toRequest(
            UUID.randomUUID().toString(),
            List.of(sellable, hidden)
        );

        assertEquals(2, request.roomTypes().size());
        assertTrue(request.toDate().isAfter(request.fromDate()));
        assertEquals(3, quantityFor("DLX", request));
        assertEquals(0, quantityFor("MNT", request));
    }

    private int quantityFor(String roomCode, InventoryReconciliationRequest request) {
        UUID expectedRoomTypeId = UUID.nameUUIDFromBytes(
            (request.propertyId().toString() + ":" + roomCode).toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
        return request.roomTypes().stream()
            .filter(roomType -> roomType.roomTypeId().equals(expectedRoomTypeId))
            .findFirst()
            .orElseThrow()
            .totalInventory();
    }
}


