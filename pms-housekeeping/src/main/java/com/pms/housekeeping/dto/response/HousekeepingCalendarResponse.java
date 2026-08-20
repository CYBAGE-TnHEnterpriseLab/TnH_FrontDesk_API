package com.pms.housekeeping.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HousekeepingCalendarResponse(
        UUID propertyId,
        LocalDate fromDate,
        LocalDate toDate,
        List<CalendarDateResponse> dates,
        List<CalendarRoomTypeResponse> roomTypes
) {
}