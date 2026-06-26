package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.service.ReservationAvailabilityService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationAvailabilityController.class)
@Import(GlobalExceptionHandler.class)
class ReservationAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationAvailabilityService reservationAvailabilityService;

    @Test
    void getAvailabilityShouldReturnLiveInventoryAndPricing() throws Exception {
        ReservationAvailabilityResponseDto response = ReservationAvailabilityResponseDto.builder()
                .propertyId("PROP001")
                .arrivalDate(LocalDate.of(2026, 7, 1))
                .departureDate(LocalDate.of(2026, 7, 3))
                .selectedRoomType("Deluxe King")
                .availability(List.of(RoomAvailabilityPricingDto.builder()
                        .roomType("Deluxe King")
                        .ratePlan("BAR")
                        .rateCode("BAR001")
                        .occupancy("2 Adults")
                        .mealPlan("BB")
                        .availableRooms(4)
                        .baseRate(new BigDecimal("5000.00"))
                        .taxAmount(new BigDecimal("900.00"))
                        .finalAmount(new BigDecimal("5900.00"))
                        .build()))
                .build();

        when(reservationAvailabilityService.getAvailability(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", "PROP001")
                        .param("arrivalDate", "2026-07-01")
                        .param("departureDate", "2026-07-03")
                        .param("roomType", "Deluxe King")
                        .param("adultCount", "2")
                        .param("childCount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Availability and pricing fetched successfully"))
                .andExpect(jsonPath("$.data.propertyId").value("PROP001"))
                .andExpect(jsonPath("$.data.availability[0].roomType").value("Deluxe King"))
                .andExpect(jsonPath("$.data.availability[0].ratePlan").value("BAR"))
                .andExpect(jsonPath("$.data.availability[0].availableRooms").value(4))
                .andExpect(jsonPath("$.data.availability[0].finalAmount").value(5900.00));

        verify(reservationAvailabilityService).getAvailability(any());
    }

    @Test
    void getAvailabilityShouldReturnBadRequestWhenPropertyIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", " ")
                        .param("arrivalDate", "2026-07-01")
                        .param("departureDate", "2026-07-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
