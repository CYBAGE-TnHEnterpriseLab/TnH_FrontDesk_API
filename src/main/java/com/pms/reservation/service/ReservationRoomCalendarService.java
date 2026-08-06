package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import java.time.LocalDate;
import java.util.List;

public interface ReservationRoomCalendarService {

    ReservationRoomCalendarResponseDto getRoomCalendar(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<String> roomTypes
    );
}
