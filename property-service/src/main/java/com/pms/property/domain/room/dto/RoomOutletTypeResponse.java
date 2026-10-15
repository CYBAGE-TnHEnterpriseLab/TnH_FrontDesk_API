package com.pms.property.domain.room.dto;

public record RoomOutletTypeResponse(
    Long id,
    String propertyId,
    String roomName,
    String roomCode,
    Integer quantity,
    Boolean availableForSell,
    Integer maximumGuestOccupancy,
    Integer minimumGuestOccupancy,
    String description,
    String amenitiesCsv,
    String imagesCsv
) {
}
