package com.pms.reservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationController.class, properties = "security.jwt.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationBookingService reservationBookingService;

    @Test
    void getPaymentModesShouldReturnSupportedModes() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/payment-modes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment modes fetched successfully"))
                .andExpect(jsonPath("$.data[0]").value("CARD"))
                .andExpect(jsonPath("$.data[1]").value("CASH"))
                .andExpect(jsonPath("$.data[2]").value("UPI"))
                .andExpect(jsonPath("$.data[3]").value("NET_BANKING"))
                .andExpect(jsonPath("$.data[4]").value("WALLET"));
    }

    @Test
    void getPaymentTypesShouldReturnSupportedTypes() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/payment-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Payment types fetched successfully"))
            .andExpect(jsonPath("$.data[0]").value("ADVANCE"))
            .andExpect(jsonPath("$.data[1]").value("FULL_PAYMENT"));
    }

    @Test
    void createBookingShouldReturnCreatedResponse() throws Exception {
        ReservationBookingRequestDto request = validRequest();

        ReservationBookingResponseDto response = ReservationBookingResponseDto.builder()
                .bookingId(1001L)
                .propertyId("PROP001")
                .guestName("Alex Johnson")
                .reservationStatus("CONFIRMED")
            .confirmationNumber("1234567890")
                .createdAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                .build();

        when(reservationBookingService.createBooking(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reservation confirmed successfully"))
                .andExpect(jsonPath("$.data.bookingId").value(1001))
                .andExpect(jsonPath("$.data.guestName").value("Alex Johnson"))
                .andExpect(jsonPath("$.data.reservationStatus").value("CONFIRMED"));

        verify(reservationBookingService).createBooking(any());
    }

        @Test
        void createBookingShouldAcceptUiObjectTimePayload() throws Exception {
        ReservationBookingResponseDto response = ReservationBookingResponseDto.builder()
            .bookingId(1002L)
            .propertyId("PROP001")
            .guestName("Alex Johnson")
            .reservationStatus("CONFIRMED")
            .confirmationNumber("1234567891")
            .createdAt(LocalDateTime.of(2026, 7, 18, 12, 0))
            .build();

        when(reservationBookingService.createBooking(any())).thenReturn(response);

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("propertyId", "PROP001");
        payloadMap.put("salutation", "Mr");
        payloadMap.put("vipTag", false);
        payloadMap.put("guestName", "Alex Johnson");
        payloadMap.put("guestNames", List.of("Alex Johnson"));
        payloadMap.put("personalEmail", "alex.personal@example.com");
        payloadMap.put("officialEmail", "alex.official@example.com");
        payloadMap.put("city", "Pune");
        payloadMap.put("country", "India");
        payloadMap.put("zipCode", "411001");
        payloadMap.put("phoneNumber", "+91-9876543210");
        payloadMap.put("mobileNumber", "+91-9876543210");
        payloadMap.put("loyaltyNumber", "LOY1234");
        payloadMap.put("company", "Contoso");
        payloadMap.put("guestGroup", "CORP");
        payloadMap.put("source", "Website");
        payloadMap.put("agent", "Online");
        payloadMap.put("arrivalDate", "2026-07-20");
        payloadMap.put("departureDate", "2026-07-22");
        payloadMap.put("adultCount", 2);
        payloadMap.put("childCount", 1);
        payloadMap.put("reservationType", "GTD");
        payloadMap.put("roomType", "Deluxe King");
        payloadMap.put("rateCode", "BAR001");
        payloadMap.put("numberOfRooms", 1);
        payloadMap.put("rate", 8500.00);
        payloadMap.put("payment", "CARD");
        payloadMap.put("paymentType", "FULL_PAYMENT");
        payloadMap.put("eta", Map.of("hour", 15, "minute", 0));
        payloadMap.put("checkOutTime", Map.of("hour", 11, "minute", 0));
        payloadMap.put("dnm", false);
        payloadMap.put("noPost", false);
        payloadMap.put("guestBalance", 0.00);
        payloadMap.put("specialRequests", "High floor");
        payloadMap.put("discount", 0.00);
        payloadMap.put("alertsMessages", "N/A");

        String payload = objectMapper.writeValueAsString(payloadMap);

        mockMvc.perform(post("/api/v1/reservations/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.bookingId").value(1002));

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
    void createBookingShouldReturnBadRequestWhenNumberOfRoomsExceedsNine() throws Exception {
        ReservationBookingRequestDto request = validRequest();
        request.setNumberOfRooms(10);

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.numberOfRooms").value("numberOfRooms must be <= 9"));
    }

    @Test
    void createBookingShouldReturnBadRequestWhenPaymentModeIsInvalid() throws Exception {
        ReservationBookingRequestDto request = validRequest();
        request.setPayment("CHEQUE");

        mockMvc.perform(post("/api/v1/reservations/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.payment", containsString("payment must be one of CARD, CASH, UPI, NET_BANKING, WALLET")));
    }

            @Test
            void createBookingShouldAcceptUiAliasFieldsAndPlaceholderPaymentType() throws Exception {
            ReservationBookingResponseDto response = ReservationBookingResponseDto.builder()
                .bookingId(1003L)
                .propertyId("PROP001")
                .guestName("pk kp")
                .reservationStatus("CONFIRMED")
                .confirmationNumber("1234567892")
                .createdAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                .build();

            when(reservationBookingService.createBooking(any())).thenReturn(response);

            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("propertyId", "PROP001");
            payloadMap.put("firstName", "pk");
            payloadMap.put("lastName", "kp");
            payloadMap.put("officialEmail", "kumar@mail.com");
            payloadMap.put("phone", "9090912345");
            payloadMap.put("arrivalDate", "2026-07-15");
            payloadMap.put("departureDate", "2026-07-17");
            payloadMap.put("adultCount", 1);
            payloadMap.put("childCount", 0);
            payloadMap.put("roomType", "DLX");
            payloadMap.put("ratePlan", "BARR");
            payloadMap.put("numberOfRooms", 1);
            payloadMap.put("rate", 1800);
            payloadMap.put("payment", "CARD");
            payloadMap.put("paymentType", "Select Payment Type");
            payloadMap.put("eta", "11:00 AM");
            payloadMap.put("checkOutTime", "11:00 AM");
            payloadMap.put("dnm", false);
            payloadMap.put("guestBalance", -1800);
            payloadMap.put("discount", 0);
            payloadMap.put("specialRequests", "High Floor");

            String payload = objectMapper.writeValueAsString(payloadMap);

            mockMvc.perform(post("/api/v1/reservations/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(1003));

            verify(reservationBookingService).createBooking(any());
            }

        @Test
        void createBookingShouldAcceptEmailAliasField() throws Exception {
            ReservationBookingResponseDto response = ReservationBookingResponseDto.builder()
                    .bookingId(1004L)
                    .propertyId("PROP001")
                    .guestName("pk kp")
                    .reservationStatus("CONFIRMED")
                    .confirmationNumber("1234567893")
                    .createdAt(LocalDateTime.of(2026, 7, 18, 12, 0))
                    .build();

            when(reservationBookingService.createBooking(any())).thenReturn(response);

            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("propertyId", "PROP001");
            payloadMap.put("firstName", "pk");
            payloadMap.put("lastName", "kp");
            payloadMap.put("email", "kumar@mail.com");
            payloadMap.put("phone", "9090912345");
            payloadMap.put("arrivalDate", "2026-07-15");
            payloadMap.put("departureDate", "2026-07-17");
            payloadMap.put("adultCount", 1);
            payloadMap.put("childCount", 0);
            payloadMap.put("roomType", "DLX");
            payloadMap.put("ratePlan", "BARR");
            payloadMap.put("numberOfRooms", 1);
            payloadMap.put("rate", 1800);
            payloadMap.put("payment", "CARD");
            payloadMap.put("paymentType", "ADVANCE");
            payloadMap.put("eta", "11:00 AM");
            payloadMap.put("checkOutTime", "11:00 AM");
            payloadMap.put("dnm", false);
            payloadMap.put("guestBalance", 1800);
            payloadMap.put("discount", 0);

            String payload = objectMapper.writeValueAsString(payloadMap);

            mockMvc.perform(post("/api/v1/reservations/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.bookingId").value(1004));

            verify(reservationBookingService).createBooking(any());
        }

    private ReservationBookingRequestDto validRequest() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPropertyId("PROP001");
        request.setSalutation("Mr");
        request.setVipTag(Boolean.FALSE);
        request.setGuestName("Alex Johnson");
        request.setGuestNames(List.of("Alex Johnson"));
        request.setPersonalEmail("alex.personal@example.com");
        request.setOfficialEmail("alex.official@example.com");
        request.setCity("Pune");
        request.setCountry("India");
        request.setZipCode("411001");
        request.setPhoneNumber("+91-9876543210");
        request.setMobileNumber("+91-9876543210");
        request.setLoyaltyNumber("LOY1234");
        request.setCompany("Contoso");
        request.setGuestGroup("CORP");
        request.setSource("Website");
        request.setAgent("Online");
        request.setArrivalDate(LocalDate.of(2026, 7, 20));
        request.setDepartureDate(LocalDate.of(2026, 7, 22));
        request.setAdultCount(2);
        request.setChildCount(1);
        request.setReservationType("GTD");
        request.setRoomType("Deluxe King");
        request.setRateCode("BAR001");
        request.setNumberOfRooms(1);
        request.setRate(new BigDecimal("8500.00"));
        request.setPayment("CARD");
        request.setPaymentType("FULL_PAYMENT");
        request.setEta(LocalTime.of(15, 0));
        request.setCheckOutTime(LocalTime.of(11, 0));
        request.setDnm(Boolean.FALSE);
        request.setNoPost(Boolean.FALSE);
        request.setGuestBalance(new BigDecimal("0.00"));
        request.setSpecialRequests("High floor");
        request.setDiscount(new BigDecimal("0.00"));
        request.setAlertsMessages("N/A");
        return request;
    }
}
