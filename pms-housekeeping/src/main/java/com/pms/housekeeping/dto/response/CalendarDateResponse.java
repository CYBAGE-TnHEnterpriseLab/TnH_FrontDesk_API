package com.pms.housekeeping.dto.response;

import java.time.LocalDate;

public record CalendarDateResponse(
        LocalDate date,
        String dayOfWeek,
        int dayOfMonth
) {
}
