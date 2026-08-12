package com.pms.housekeeping.dto.response;

import java.util.List;
import java.util.UUID;

public record CalendarRoomTypeResponse(
        UUID roomTypeId,
        String roomTypeName,
        List<CalendarRoomResponse> rooms
) {
}
