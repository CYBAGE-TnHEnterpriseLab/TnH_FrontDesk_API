package com.pms.housekeeping.dto.response;

import java.time.LocalDate;

public record CalendarRoomDayResponse(
        LocalDate date,
        String cleaningStatus,
        String frontOfficeStatus,
        String reservationStatus,
        String guestDisplayName,
        LocalDate arrivalDate,
        LocalDate departureDate,
        String attendantName,
        String priority,
        Boolean sellable,
        String assignedReservationId
) {
}