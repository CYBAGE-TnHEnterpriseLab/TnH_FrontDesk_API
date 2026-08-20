package com.pms.housekeeping.dto.response;

import java.util.List;

public record CalendarRoomResponse(
        String roomNumber,
        String floor,
        List<CalendarRoomDayResponse> days
) {
}
