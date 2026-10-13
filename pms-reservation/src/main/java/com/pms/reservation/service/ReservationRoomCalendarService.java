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

    default ReservationRoomCalendarResponseDto getRoomCalendar(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<String> roomTypes,
            Integer adults,
            Integer children,
            String rateCode
        ) {
        return getRoomCalendar(propertyId, arrivalDate, departureDate, roomTypes,
            adults, children, rateCode, null, null);
        }

        default ReservationRoomCalendarResponseDto getRoomCalendar(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            List<String> roomTypes,
            Integer adults,
            Integer children,
            String rateCode,
            Integer ageOfChild1,
            Integer ageOfChild2
    ) {
        return getRoomCalendar(propertyId, arrivalDate, departureDate, roomTypes);
    }
}
