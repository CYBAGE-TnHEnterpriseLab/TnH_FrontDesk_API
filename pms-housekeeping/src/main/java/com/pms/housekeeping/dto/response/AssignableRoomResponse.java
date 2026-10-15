package com.pms.housekeeping.dto.response;


public record AssignableRoomResponse(
        String roomNumber,
        String roomTypeId,
        String roomTypeName,
        String floor,
        String roomClass,
        String zone,
        String cleaningStatus
) {
}


