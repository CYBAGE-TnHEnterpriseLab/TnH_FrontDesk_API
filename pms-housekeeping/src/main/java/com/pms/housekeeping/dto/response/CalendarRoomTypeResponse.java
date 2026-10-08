package com.pms.housekeeping.dto.response;

import java.util.List;

public record CalendarRoomTypeResponse(
        String roomTypeId,
        String roomTypeName,
        List<CalendarRoomResponse> rooms
) {
}
