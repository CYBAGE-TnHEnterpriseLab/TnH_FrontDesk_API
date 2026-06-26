package com.pms.reservation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationBookingMapperTest {

    private final ReservationBookingMapper mapper = new ReservationBookingMapper();

    @Test
    void toEntityShouldSanitizeGuestNamesCalculateTotalRateAndSelectPrimaryGuest() {
        ReservationBookingRequestDto request = validRequest();
        request.setGuestName(" ");
        request.setGuestNames(List.of(" Alex Johnson ", " ", "Priya Rao"));
        request.setRate(new BigDecimal("2500.00"));
        request.setNumberOfRooms(2);

        ReservationBookingRecord entity = mapper.toEntity(request);

        assertThat(entity.getGuestName()).isEqualTo("Alex Johnson");
        assertThat(entity.getGuestNamesEncoded()).isNotBlank();
        assertThat(entity.getTotalRate()).isEqualByComparingTo("5000.00");
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    void toEntityShouldSetTotalRateToZeroWhenRateOrRoomCountMissing() {
        ReservationBookingRequestDto request = validRequest();
        request.setRate(null);

        ReservationBookingRecord entity = mapper.toEntity(request);

        assertThat(entity.getTotalRate()).isEqualByComparingTo("0");
    }

    @Test
    void toResponseShouldDecodeAllGuestNames() {
        ReservationBookingRecord record = ReservationBookingRecord.builder()
                .id(42L)
                .confirmationNumber("PROP001-20260626111111111-123")
                .reservationStatus("CONFIRMED")
                .propertyId("PROP001")
                .salutation("Mr.")
                .vipTag(Boolean.FALSE)
                .guestName("Alex Johnson")
                .guestNamesEncoded("QWxleCBKb2huc29u.UHJpeWEgUmFv")
                .arrivalDate(LocalDate.of(2026, 6, 20))
                .departureDate(LocalDate.of(2026, 6, 22))
                .adultCount(2)
                .childCount(1)
                .reservationType("GTD")
                .roomType("Deluxe King")
                .rateCode("BAR")
                .numberOfRooms(2)
                .rate(new BigDecimal("2500.00"))
                .totalRate(new BigDecimal("5000.00"))
                .payment("Card")
                .eta(LocalTime.of(15, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .dnm(Boolean.FALSE)
                .noPost(Boolean.FALSE)
                .guestBalance(new BigDecimal("0.00"))
                .discount(new BigDecimal("0.00"))
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        ReservationBookingResponseDto response = mapper.toResponse(record);

        assertThat(response.getBookingId()).isEqualTo(42L);
        assertThat(response.getGuestNames()).containsExactly("Alex Johnson", "Priya Rao");
        assertThat(response.getTotalRate()).isEqualByComparingTo("5000.00");
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
