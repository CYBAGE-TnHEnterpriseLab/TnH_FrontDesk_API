package com.pms.reservation.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationRoomCalendarResponseDto {
    String propertyId;
    LocalDate arrivalDate;
    LocalDate departureDate;
    List<String> roomTypes;
    List<LocalDate> dates;
    List<RoomCalendarRowDto> rooms;
    List<RoomCalendarDaySummaryDto> summary;

    @Value
    @Builder
    public static class RoomCalendarRowDto {
        String roomNo;
        String roomType;
        Integer floor;
        List<RoomCalendarCellDto> calendar;
    }

    @Value
    @Builder
    public static class RoomCalendarCellDto {
        LocalDate date;
        String status;
        String confirmationNumber;
        Long bookingId;
        String reservationStatus;
    }

    @Value
    @Builder
    public static class RoomCalendarDaySummaryDto {
        LocalDate date;
        Integer totalRooms;
        Integer assignableRooms;
        Integer availableRooms;
        Integer bookedRooms;
        Integer occupiedRooms;
        Integer dirtyRooms;
        Integer cleanedRooms;
    }
}
