package com.pms.guestlisting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.repository.ReservationBookingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GuestListingController.class, properties = "security.jwt.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GuestListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationBookingRepository reservationBookingRepository;

        @MockBean
        private HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;

    @Test
    void getGuestListingShouldReturnBookingsFromSingleTableByDefault() throws Exception {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(1L)
                .propertyId("PROP001")
                .confirmationNumber("CNF458721")
                .reservationStatus("CONFIRMED")
                .salutation("Mr")
                .vipTag(false)
                .guestName("John Smith")
                .guestNamesEncoded("[\"John Smith\"]")
                .personalEmail("john.personal@example.com")
                .officialEmail("john.official@example.com")
                .city("Mumbai")
                .country("India")
                .zipCode("400001")
                .phoneNumber("1234567890")
                .mobileNumber("1234567890")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 3))
                .adultCount(2)
                .childCount(1)
                .reservationType("GTD")
                .roomType("Deluxe King")
                .assignedRoomNo("305")
                .floor(3)
                .rateCode("BAR")
                .numberOfRooms(1)
                .rate(new BigDecimal("5000.00"))
                .totalRate(new BigDecimal("10000.00"))
                .payment("CARD")
                .eta(java.time.LocalTime.of(14, 0))
                .checkOutTime(java.time.LocalTime.of(11, 0))
                .dnm(false)
                .noPost(false)
                .guestBalance(new BigDecimal("0.00"))
                .discount(new BigDecimal("0.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<ReservationBookingRecord> page = new PageImpl<>(
                java.util.List.of(booking),
                PageRequest.of(0, 20),
                1
        );
        when(reservationBookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(housekeepingRoomStatusRepository.findByPropertyIdAndBusinessDateAndConfirmationNumberIn(
                any(),
                any(),
                anyCollection()
        )).thenReturn(List.of(HousekeepingRoomStatusRecord.builder()
                .propertyId("PROP001")
                .businessDate(LocalDate.of(2026, 6, 1))
                .confirmationNumber("CNF458721")
                .roomNo("301")
                .roomStatus("OCCUPIED")
                .updatedAt(LocalDateTime.now())
                .build()));

        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Guest listing fetched successfully"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].confirmationNumber").value("CNF458721"))
                .andExpect(jsonPath("$.data.content[0].guests").value(3))
                .andExpect(jsonPath("$.data.content[0].roomNo").value("301"))
                .andExpect(jsonPath("$.data.content[0].roomStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.data.content[0].floor").value(3));

        verify(reservationBookingRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
        verify(housekeepingRoomStatusRepository).findByPropertyIdAndBusinessDateAndConfirmationNumberIn(
                any(),
                any(),
                anyCollection()
        );
    }

    @Test
    void getGuestListingShouldUseAssignedRoomNoWhenHousekeepingRoomNotAvailable() throws Exception {
        ReservationBookingRecord booking = ReservationBookingRecord.builder()
                .id(2L)
                .propertyId("PROP001")
                .confirmationNumber("CNF458722")
                .reservationStatus("CONFIRMED")
                .salutation("Ms")
                .vipTag(false)
                .guestName("Jane Doe")
                .phoneNumber("1234567890")
                .mobileNumber("1234567890")
                .arrivalDate(LocalDate.of(2026, 6, 1))
                .departureDate(LocalDate.of(2026, 6, 2))
                .adultCount(1)
                .childCount(0)
                .reservationType("GTD")
                .roomType("Deluxe King")
                .assignedRoomNo("410")
                .floor(4)
                .rateCode("BAR")
                .numberOfRooms(1)
                .rate(new BigDecimal("5000.00"))
                .totalRate(new BigDecimal("5000.00"))
                .payment("CARD")
                .eta(java.time.LocalTime.of(14, 0))
                .checkOutTime(java.time.LocalTime.of(11, 0))
                .dnm(false)
                .noPost(false)
                .guestBalance(new BigDecimal("0.00"))
                .discount(new BigDecimal("0.00"))
                .createdAt(LocalDateTime.now())
                .build();

        Page<ReservationBookingRecord> page = new PageImpl<>(
                java.util.List.of(booking),
                PageRequest.of(0, 20),
                1
        );
        when(reservationBookingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(housekeepingRoomStatusRepository.findByPropertyIdAndBusinessDateAndConfirmationNumberIn(
                any(),
                any(),
                anyCollection()
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].confirmationNumber").value("CNF458722"))
                .andExpect(jsonPath("$.data.content[0].roomNo").value("410"))
                .andExpect(jsonPath("$.data.content[0].floor").value(4));
    }

    @Test
    void getGuestListingShouldReturnBadRequestWhenViewIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/guest-listing/list")
                        .param("propertyId", "PROP001")
                        .param("businessDate", "2026-06-01")
                        .param("view", "both"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
