package com.pms.property.domain.room.dto;

public record InventoryRoomRequest(
    String floorName,
    String roomTypeName,
    String roomNumber
) {
}

