package com.pms.reservation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReservationBookingRequestDtoTest {

  private final ObjectMapper objectMapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

    @Test
    void shouldComposeGuestNameFromFirstAndLastNameRegardlessOfOrder() throws Exception {
        String payloadFirstLast = """
                {
                  "firstName": "pk",
                  "lastName": "kp",
                  "paymentType": "Select Payment Type",
                  "guestBalance": -1800,
                  "eta": "11:00:00",
                  "checkOutTime": "11:00:00",
                  "propertyId": "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                  "phoneNumber": "9090912345",
                  "arrivalDate": "2026-07-15",
                  "departureDate": "2026-07-16",
                  "adultCount": 1,
                  "childCount": 0,
                  "roomType": "DLX",
                  "rateCode": "BARR",
                  "numberOfRooms": 1,
                  "rate": 1800,
                  "payment": "CARD",
                  "dnm": false,
                  "discount": 0
                }
                """;

        ReservationBookingRequestDto dtoA = objectMapper.readValue(payloadFirstLast, ReservationBookingRequestDto.class);
        assertThat(dtoA.getGuestName()).isEqualTo("pk kp");
        assertThat(dtoA.getPaymentType()).isNull();
        assertThat(dtoA.getGuestBalance()).isEqualByComparingTo(new BigDecimal("1800"));

        String payloadLastFirst = """
                {
                  "lastName": "kp",
                  "firstName": "pk",
                  "paymentType": "Select Payment Type",
                  "guestBalance": -1800,
                  "eta": "11:00:00",
                  "checkOutTime": "11:00:00",
                  "propertyId": "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                  "phoneNumber": "9090912345",
                  "arrivalDate": "2026-07-15",
                  "departureDate": "2026-07-16",
                  "adultCount": 1,
                  "childCount": 0,
                  "roomType": "DLX",
                  "rateCode": "BARR",
                  "numberOfRooms": 1,
                  "rate": 1800,
                  "payment": "CARD",
                  "dnm": false,
                  "discount": 0
                }
                """;

        ReservationBookingRequestDto dtoB = objectMapper.readValue(payloadLastFirst, ReservationBookingRequestDto.class);
        assertThat(dtoB.getGuestName()).isEqualTo("pk kp");
    }

    @Test
    void shouldKeepExplicitGuestNameWhenProvided() throws Exception {
        String payload = """
                {
                  "guestName": "Explicit Guest",
                  "firstName": "pk",
                  "lastName": "kp",
                  "paymentType": "Select Payment Type",
                  "guestBalance": -1800,
                  "eta": "11:00:00",
                  "checkOutTime": "11:00:00",
                  "propertyId": "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
                  "phoneNumber": "9090912345",
                  "arrivalDate": "2026-07-15",
                  "departureDate": "2026-07-16",
                  "adultCount": 1,
                  "childCount": 0,
                  "roomType": "DLX",
                  "rateCode": "BARR",
                  "numberOfRooms": 1,
                  "rate": 1800,
                  "payment": "CARD",
                  "dnm": false,
                  "discount": 0
                }
                """;

        ReservationBookingRequestDto dto = objectMapper.readValue(payload, ReservationBookingRequestDto.class);
        assertThat(dto.getGuestName()).isEqualTo("Explicit Guest");
    }

  @Test
  void shouldDeserializeRoomAssignmentAliases() throws Exception {
    String payload = """
        {
          "guestName": "Alex Johnson",
          "paymentType": "FULL_PAYMENT",
          "guestBalance": 1800,
          "eta": "11:00:00",
          "checkOutTime": "11:00:00",
          "propertyId": "7cfd4559-b6f3-4b7d-b933-e93018ac1d47",
          "phoneNumber": "9090912345",
          "arrivalDate": "2026-07-15",
          "departureDate": "2026-07-16",
          "adultCount": 1,
          "childCount": 0,
          "roomType": "DLX",
          "roomNo": "502",
          "floor": 5,
          "rateCode": "BARR",
          "numberOfRooms": 1,
          "rate": 1800,
          "payment": "CARD",
          "dnm": false,
          "discount": 0
        }
        """;

    ReservationBookingRequestDto dto = objectMapper.readValue(payload, ReservationBookingRequestDto.class);

    assertThat(dto.getAssignedRoomNo()).isEqualTo("502");
    assertThat(dto.getFloor()).isEqualTo(5);
  }
}
