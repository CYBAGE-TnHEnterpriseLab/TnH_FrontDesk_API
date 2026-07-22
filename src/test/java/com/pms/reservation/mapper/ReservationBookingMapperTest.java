package com.pms.reservation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationBookingMapperTest {

    private final ReservationBookingMapper mapper = new ReservationBookingMapper();

    @Test
    void toEntityShouldMapCurrentFieldsAndCalculateTotalRate() {
        ReservationBookingRequestDto request = validRequest();
        request.setRate(new BigDecimal("2500.00"));
        request.setNumberOfRooms(2);

        ReservationBookingRecord entity = mapper.toEntity(request);

        assertThat(entity.getGuestName()).isEqualTo("Alex Johnson");
        assertThat(entity.getSource()).isEqualTo("Website");
        assertThat(entity.getRateCode()).isEqualTo("BAR001");
        assertThat(entity.getGuestNamesEncoded()).isNotBlank();
        assertThat(entity.getTotalRate()).isEqualByComparingTo("10000.00");
        assertThat(entity.getPaymentType()).isEqualTo("FULL_PAYMENT");
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void toEntityShouldFallbackPrimaryGuestNameFromGuestNamesWhenGuestNameMissing() {
        ReservationBookingRequestDto request = validRequest();
        request.setGuestName(" ");
        request.setGuestNames(List.of("Priya Rao"));

        ReservationBookingRecord entity = mapper.toEntity(request);

        assertThat(entity.getGuestName()).isEqualTo("Priya Rao");
    }

    @Test
    void toEntityShouldSetTotalRateToZeroWhenStayNightsAreZero() {
        ReservationBookingRequestDto request = validRequest();
        request.setArrivalDate(LocalDate.of(2026, 6, 20));
        request.setDepartureDate(LocalDate.of(2026, 6, 20));
        request.setRate(new BigDecimal("2500.00"));
        request.setNumberOfRooms(2);

        ReservationBookingRecord entity = mapper.toEntity(request);

        assertThat(entity.getTotalRate()).isEqualByComparingTo("0");
    }

    @Test
    void toResponseShouldDecodeGuestNamesAndExposePaymentTransactionFields() {
        ReservationBookingRecord record = ReservationBookingRecord.builder()
                .id(42L)
                .confirmationNumber("PROP001-20260626111111111-123")
                .reservationStatus("CONFIRMED")
                .propertyId("PROP001")
                .salutation("Mr")
                .vipTag(Boolean.FALSE)
                .guestName("Alex Johnson")
                .guestNamesEncoded("QWxleCBKb2huc29u.UHJpeWEgUmFv")
                .personalEmail("alex.personal@example.com")
                .officialEmail("alex.official@example.com")
                .city("Pune")
                .country("India")
                .zipCode("411001")
                .phoneNumber("+91-9876543210")
                .mobileNumber("+91-9876543210")
                .arrivalDate(LocalDate.of(2026, 6, 20))
                .departureDate(LocalDate.of(2026, 6, 22))
                .adultCount(2)
                .childCount(1)
                .reservationType("GTD")
                .roomType("Deluxe King")
                .rateCode("BAR001")
                .numberOfRooms(2)
                .rate(new BigDecimal("2500.00"))
                .totalRate(new BigDecimal("10000.00"))
                .payment("CARD")
                .paymentType("FULL_PAYMENT")
                .eta(LocalTime.of(15, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .dnm(Boolean.FALSE)
                .noPost(Boolean.FALSE)
                .guestBalance(new BigDecimal("0.00"))
                .discount(new BigDecimal("0.00"))
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        ReservationPaymentTransactionRecord txn = ReservationPaymentTransactionRecord.builder()
                .transactionStatus("SUCCESS")
                .transactionReference("PAY-777")
                .processorName("SIMULATED_GATEWAY")
                .processedAt(LocalDateTime.of(2026, 6, 1, 10, 1))
                .build();

        ReservationBookingResponseDto response = mapper.toResponse(record, txn);

        assertThat(response.getBookingId()).isEqualTo(42L);
        assertThat(response.getGuestNames()).containsExactly("Alex Johnson", "Priya Rao");
        assertThat(response.getTotalRate()).isEqualByComparingTo("10000.00");
        assertThat(response.getPaymentTransactionStatus()).isEqualTo("SUCCESS");
        assertThat(response.getPaymentTransactionReference()).isEqualTo("PAY-777");
        assertThat(response.getPaymentProcessorName()).isEqualTo("SIMULATED_GATEWAY");
        assertThat(response.getPaymentType()).isEqualTo("FULL_PAYMENT");
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
        request.setArrivalDate(LocalDate.of(2026, 6, 20));
        request.setDepartureDate(LocalDate.of(2026, 6, 22));
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
