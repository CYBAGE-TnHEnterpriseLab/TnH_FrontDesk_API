package com.pms.property.domain.room.dto;

public record InventoryRoomResponse(
    Long id,
    String propertyId,
    String floorName,
    String roomTypeName,
    String roomNumber
) {
}

