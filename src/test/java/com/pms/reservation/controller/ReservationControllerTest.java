package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.service.ReservationBookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationBookingService reservationBookingService;

    @Test
    void createBookingShouldReturnCreatedResponse() throws Exception {
        ReservationBookingRequestDto request = validRequest();

        ReservationBookingResponseDto response = ReservationBookingResponseDto.builder()
                .bookingId(1001L)
                .propertyId("PROP001")
                .guestName("Alex Johnson")
                .reservationType("GTD")
                .createdAt(LocalDateTime.of(2026, 6, 15, 12, 0))
                .build();

        when(reservationBookingService.createBooking(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                .andExpect(jsonPath("$.data.bookingId").value(1001))
                .andExpect(jsonPath("$.data.guestName").value("Alex Johnson"));

        verify(reservationBookingService).createBooking(any());
    }

    @Test
    void createBookingShouldReturnBadRequestWhenGuestNameMissing() throws Exception {
        ReservationBookingRequestDto request = validRequest();
        request.setGuestName(" ");

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.guestName").value("guestName is required"));
    }

    @Test
    void createBookingShouldReturnBadRequestWhenGuestNamesMissing() throws Exception {
        ReservationBookingRequestDto request = validRequest();
        request.setGuestNames(null);

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.guestNames").value("guestNames is required"));
    }

            @Test
            void createBookingShouldReturnBadRequestWhenNumberOfRoomsExceedsNine() throws Exception {
            ReservationBookingRequestDto request = validRequest();
            request.setNumberOfRooms(10);
            request.setGuestNames(List.of(
                "Guest 1", "Guest 2", "Guest 3", "Guest 4", "Guest 5",
                "Guest 6", "Guest 7", "Guest 8", "Guest 9", "Guest 10"
            ));

            mockMvc.perform(post("/api/v1/reservations/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.numberOfRooms").value("numberOfRooms must be <= 9"));
            }

    private ReservationBookingRequestDto validRequest() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr.");
        request.setVipTag(Boolean.FALSE);
        request.setGuestName("Alex Johnson");
        request.setGuestNames(List.of("Alex Johnson"));
        request.setPersonalEmail("alex.personal@example.com");
        request.setOfficialEmail("alex.official@example.com");
        request.setCity("Mumbai");
        request.setCountry("India");
        request.setZipCode("400001");
        request.setPhoneNumber("+91-22-1234567");
        request.setMobileNumber("+91-9876543210");
        request.setLoyaltyNumber("LOY1234");
        request.setCompany("Contoso");
        request.setGuestGroup("Corporate");
        request.setSource("Website");
        request.setAgent("Agent A");
        request.setArrivalDate(LocalDate.of(2026, 6, 20));
        request.setDepartureDate(LocalDate.of(2026, 6, 22));
        request.setAdultCount(2);
        request.setChildCount(1);
        request.setReservationType("GTD");
        request.setRoomType("Deluxe King");
        request.setRateCode("BAR");
        request.setNumberOfRooms(1);
        request.setRate(new BigDecimal("8500.00"));
        request.setPayment("Card");
        request.setEta(LocalTime.of(15, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        request.setDnm(Boolean.FALSE);
        request.setNoPost(Boolean.FALSE);
        request.setGuestBalance(new BigDecimal("0.00"));
        request.setSpecialRequests("High floor");
        request.setDiscount(new BigDecimal("500.00"));
        request.setAlertsMessages("Guest requested quiet room");
        return request;
    }
}
