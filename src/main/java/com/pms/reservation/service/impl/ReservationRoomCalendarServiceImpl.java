package com.pms.reservation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.pms.reservation.service.ReservationRoomCalendarService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service @RequiredArgsConstructor
public class ReservationRoomCalendarServiceImpl implements ReservationRoomCalendarService {
  private final HousekeepingRoomCalendarClient housekeeping;
  public ReservationRoomCalendarResponseDto getRoomCalendar(String propertyId, LocalDate arrival, LocalDate departure, List<String> roomTypes) {
    if (!StringUtils.hasText(propertyId) || arrival == null || departure == null || !departure.isAfter(arrival)) throw new BadRequestException("propertyId, arrivalDate and a later departureDate are required");
    JsonNode root=housekeeping.fetchCalendar(propertyId,arrival,departure.minusDays(1),null); List<LocalDate> dates=new ArrayList<>(); List<ReservationRoomCalendarResponseDto.RoomCalendarRowDto> rooms=new ArrayList<>();
    for(JsonNode d:root.path("dates")) dates.add(LocalDate.parse(d.path("date").asText()));
    for(JsonNode type:root.path("roomTypes")){String name=type.path("roomTypeName").asText(); if(roomTypes!=null&&!roomTypes.isEmpty()&&roomTypes.stream().noneMatch(x->name.equalsIgnoreCase(x)))continue; for(JsonNode room:type.path("rooms")){List<ReservationRoomCalendarResponseDto.RoomCalendarCellDto> cells=new ArrayList<>(); for(JsonNode day:room.path("days"))cells.add(ReservationRoomCalendarResponseDto.RoomCalendarCellDto.builder().date(LocalDate.parse(day.path("date").asText())).status(status(day)).confirmationNumber(day.path("confirmationNumber").asText(null)).reservationStatus(day.path("reservationStatus").asText(null)).build()); rooms.add(ReservationRoomCalendarResponseDto.RoomCalendarRowDto.builder().roomNo(room.path("roomNumber").asText()).roomType(name).calendar(cells).build());}}
    return ReservationRoomCalendarResponseDto.builder().propertyId(propertyId).arrivalDate(arrival).departureDate(departure).roomTypes(roomTypes==null?List.of():roomTypes).dates(dates).rooms(rooms).summary(List.of()).build();
  }
  private String status(JsonNode d){if(d.path("sellable").asBoolean(false)&&"NOT_RESERVED".equalsIgnoreCase(d.path("reservationStatus").asText()))return "AVAILABLE";if(!"NOT_RESERVED".equalsIgnoreCase(d.path("reservationStatus").asText()))return "BOOKED";if("OCCUPIED".equalsIgnoreCase(d.path("frontOfficeStatus").asText()))return "OCCUPIED";if("DIRTY".equalsIgnoreCase(d.path("cleaningStatus").asText()))return "DIRTY";return "CLEANED";}
}
