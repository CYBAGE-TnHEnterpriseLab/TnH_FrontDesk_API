package com.pms.property.domain.room.dto;

public record RoomSummaryResponse(
    String propertyId,
    long floorConfigurationsCount,
    long propertyAreasCount,
    long floorPropertyAreasCount,
    long roomOutletTypesCount,
    long inventoryRoomsCount
) {
}

