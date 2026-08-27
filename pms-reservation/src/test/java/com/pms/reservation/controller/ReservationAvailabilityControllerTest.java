package com.pms.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.dto.DailyAvailabilityPricingDto;
import com.pms.reservation.dto.RatePlanAvailabilityDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;
import com.pms.reservation.dto.RoomAvailabilityPricingDto;
import com.pms.reservation.service.ReservationAvailabilityService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationAvailabilityController.class, properties = "security.jwt.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationAvailabilityService reservationAvailabilityService;

    @Test
    void getAvailabilityShouldReturnLiveInventoryAndPricing() throws Exception {
        ReservationAvailabilityResponseDto response = ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .arrivalDate(LocalDate.of(2026, 7, 1))
                .departureDate(LocalDate.of(2026, 7, 3))
                .night(2)
                .numberOfRooms(1)
                .adults(2)
                .children(1)
                .ageOfChild1(5)
                .groupCode("GRP001")
                .company("Contoso")
                .rateCode("BAR001")
                .availableRateCodes(List.of("BAR001", "CORP"))
                .ratePlans(List.of(RatePlanAvailabilityDto.builder()
                        .ratePlan("BAR")
                        .rateCode("BAR001")
                        .roomTypes(List.of(RoomAvailabilityPricingDto.builder()
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
                        .build()))
                .blockCode("BLK01")
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
                .next15DaysPricing(List.of(DailyAvailabilityPricingDto.builder()
                        .date(LocalDate.of(2026, 7, 1))
                        .availability(List.of(RoomAvailabilityPricingDto.builder()
                                .roomType("Deluxe King")
                                .ratePlan("BAR")
                                .rateCode("BAR001")
                                .availableRooms(4)
                                .baseRate(new BigDecimal("5000.00"))
                                .taxAmount(new BigDecimal("900.00"))
                                .finalAmount(new BigDecimal("5900.00"))
                                .build()))
                        .build()))
                .build();

        when(reservationAvailabilityService.getAvailability(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", "7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .param("date", "2026-07-01")
                        .param("night", "2")
                        .param("numberOfRooms", "1")
                        .param("groupCode", "GRP001")
                        .param("company", "Contoso")
                        .param("rateCode", "BAR001")
                        .param("blockCode", "BLK01")
                        .param("adults", "2")
                        .param("children", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Availability and pricing fetched successfully"))
                .andExpect(jsonPath("$.data.propertyId").value("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"))
                .andExpect(jsonPath("$.data.night").value(2))
                .andExpect(jsonPath("$.data.numberOfRooms").value(1))
                .andExpect(jsonPath("$.data.groupCode").value("GRP001"))
                .andExpect(jsonPath("$.data.availableRateCodes[0]").value("BAR001"))
                .andExpect(jsonPath("$.data.availableRateCodes[1]").value("CORP"))
                .andExpect(jsonPath("$.data.ratePlans[0].ratePlan").value("BAR"))
                .andExpect(jsonPath("$.data.ratePlans[0].roomTypes[0].roomType").value("Deluxe King"))
                .andExpect(jsonPath("$.data.ratePlans[0].roomTypes[0].finalAmount").value(5900.00))
                .andExpect(jsonPath("$.data.availability").doesNotExist())
                .andExpect(jsonPath("$.data.next15DaysPricing[0].date").value("2026-07-01"))
                .andExpect(jsonPath("$.data.next15DaysPricing[0].availability[0].roomType").value("Deluxe King"));

        verify(reservationAvailabilityService).getAvailability(any());
    }

    @Test
    void getAvailabilityShouldReturnBadRequestWhenPropertyIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", " ")
                                                .param("date", "2026-07-01")
                                                .param("night", "2")
                                                .param("numberOfRooms", "1")
                                                .param("groupCode", "GRP001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

        @Test
        void getAvailabilityShouldAllowMissingGroupCode() throws Exception {
                ReservationAvailabilityResponseDto response = ReservationAvailabilityResponseDto.builder()
                                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                                .arrivalDate(LocalDate.of(2026, 7, 1))
                                .departureDate(LocalDate.of(2026, 7, 3))
                                .night(2)
                                .numberOfRooms(1)
                                .groupCode(null)
                                .adults(2)
                                .children(1)
                                .availableRateCodes(List.of())
                                .availability(List.of())
                                .next15DaysPricing(List.of())
                                .build();

                when(reservationAvailabilityService.getAvailability(any())).thenReturn(response);

                mockMvc.perform(get("/api/v1/reservations/availability")
                                                .param("propertyId", "7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                                                .param("date", "2026-07-01")
                                                .param("night", "2")
                                                .param("numberOfRooms", "1")
                                                .param("adults", "2")
                                                .param("children", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.propertyId").value("7cfd4559-b6f3-4b7d-b933-e93018ac1d47"));

                verify(reservationAvailabilityService).getAvailability(any());
        }

    @Test
    void getAvailabilityShouldRejectLegacyDateRangeParams() throws Exception {
        ReservationAvailabilityResponseDto response = ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .arrivalDate(LocalDate.of(2026, 7, 1))
                .departureDate(LocalDate.of(2026, 7, 3))
                .night(2)
                .numberOfRooms(1)
                .adults(2)
                .children(1)
                .availableRateCodes(List.of("BAR001"))
                .ratePlans(List.of(RatePlanAvailabilityDto.builder()
                        .ratePlan("BAR")
                        .rateCode("BAR001")
                        .roomTypes(List.of())
                        .build()))
                .next15DaysPricing(List.of())
                .build();

        when(reservationAvailabilityService.getAvailability(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", "7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .param("arrivalDate", "2026-07-01")
                        .param("departureDate", "2026-07-03")
                        .param("adultCount", "2")
                        .param("childCount", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ratePlans[0].ratePlan").value("BAR"));

        verify(reservationAvailabilityService).getAvailability(any());
    }

    @Test
    void getAvailabilityShouldDefaultNumberOfRoomsWhenMissing() throws Exception {
        ReservationAvailabilityResponseDto response = ReservationAvailabilityResponseDto.builder()
                .propertyId("7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                .arrivalDate(LocalDate.of(2026, 7, 1))
                .departureDate(LocalDate.of(2026, 7, 3))
                .night(2)
                .numberOfRooms(1)
                .adults(2)
                .children(0)
                .availableRateCodes(List.of("BAR001"))
                .ratePlans(List.of(RatePlanAvailabilityDto.builder()
                        .ratePlan("BAR")
                        .rateCode("BAR001")
                        .roomTypes(List.of())
                        .build()))
                .next15DaysPricing(List.of())
                .build();

        when(reservationAvailabilityService.getAvailability(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("propertyId", "7cfd4559-b6f3-4b7d-b933-e93018ac1d47")
                        .param("date", "2026-07-01")
                        .param("night", "2")
                        .param("adults", "2")
                        .param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.numberOfRooms").value(1))
                .andExpect(jsonPath("$.data.ratePlans[0].ratePlan").value("BAR"));

        verify(reservationAvailabilityService).getAvailability(any());
    }
}
