package com.pms.housekeeping.dto.response;

import java.time.LocalDate;
import java.util.List;

public record HousekeepingCalendarResponse(
        String propertyId,
        LocalDate fromDate,
        LocalDate toDate,
        List<CalendarDateResponse> dates,
        List<CalendarRoomTypeResponse> roomTypes
) {
}