package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.ReservationRoomCalendarResponseDto;
import com.pms.reservation.service.ReservationRoomCalendarService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationRoomCalendarController.class, properties = "security.jwt.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationRoomCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationRoomCalendarService reservationRoomCalendarService;

    @Test
    void getRoomCalendarShouldReturnRoomWiseCalendar() throws Exception {
        ReservationRoomCalendarResponseDto response = ReservationRoomCalendarResponseDto.builder()
                .propertyId("PROP001")
                .arrivalDate(LocalDate.of(2026, 8, 1))
                .departureDate(LocalDate.of(2026, 8, 3))
                .roomTypes(List.of("King", "Suite"))
                .dates(List.of(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 2),
                        LocalDate.of(2026, 8, 3)
                ))
                .rooms(List.of(
                        ReservationRoomCalendarResponseDto.RoomCalendarRowDto.builder()
                                .roomNo("101")
                                .roomType("King")
                                .floor(1)
                                .calendar(List.of(
                                        ReservationRoomCalendarResponseDto.RoomCalendarCellDto.builder()
                                                .date(LocalDate.of(2026, 8, 1))
                                                .status("BOOKED")
                                                .confirmationNumber("C-101")
                                                .bookingId(501L)
                                                .reservationStatus("CONFIRMED")
                                                .build()
                                ))
                                .build()
                ))
                .summary(List.of(
                        ReservationRoomCalendarResponseDto.RoomCalendarDaySummaryDto.builder()
                                .date(LocalDate.of(2026, 8, 1))
                                .totalRooms(1)
                                .assignableRooms(0)
                                .availableRooms(0)
                                .bookedRooms(1)
                                .occupiedRooms(0)
                                .dirtyRooms(0)
                                .cleanedRooms(0)
                                .build()
                ))
                .build();

        when(reservationRoomCalendarService.getRoomCalendar(
                eq("PROP001"),
                eq(LocalDate.of(2026, 8, 1)),
                eq(LocalDate.of(2026, 8, 3)),
                eq(List.of("King", "Suite"))
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/rooms/calendar")
                        .param("propertyId", "PROP001")
                        .param("arrivalDate", "2026-08-01")
                        .param("departureDate", "2026-08-03")
                        .param("roomTypes", "King", "Suite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Room calendar fetched successfully"))
                .andExpect(jsonPath("$.data.propertyId").value("PROP001"))
                .andExpect(jsonPath("$.data.roomTypes[0]").value("King"))
                .andExpect(jsonPath("$.data.rooms[0].roomNo").value("101"))
                .andExpect(jsonPath("$.data.rooms[0].calendar[0].status").value("BOOKED"));

        verify(reservationRoomCalendarService).getRoomCalendar(
                "PROP001",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of("King", "Suite")
        );
    }

    @Test
    void getRoomCalendarShouldValidatePropertyId() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/rooms/calendar")
                        .param("propertyId", " ")
                        .param("arrivalDate", "2026-08-01")
                        .param("departureDate", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
