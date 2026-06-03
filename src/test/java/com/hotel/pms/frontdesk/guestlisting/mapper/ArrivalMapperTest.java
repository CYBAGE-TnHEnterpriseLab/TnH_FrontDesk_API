package com.hotel.pms.frontdesk.guestlisting.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotel.pms.frontdesk.guestlisting.dto.ReservationArrivalDto;
import com.hotel.pms.frontdesk.guestlisting.entity.ArrivalRecord;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ArrivalMapperTest {

    private final ArrivalMapper mapper = new ArrivalMapper();

    @Test
    void toEntityShouldMapStatusAndPreserveProvidedRoomNights() {
        ReservationArrivalDto source = new ReservationArrivalDto();
        source.setStatus("DNM");
        source.setFirstName("John");
        source.setLastName("Smith");
        source.setConfirmationNumber("CNF-1001");
        source.setCheckInDate(LocalDate.of(2026, 6, 3));
        source.setCheckOutDate(LocalDate.of(2026, 6, 5));
        source.setRoomNights(7);

        ArrivalRecord record = mapper.toEntity(source, "PROP001", LocalDate.of(2026, 6, 3));

        assertThat(record.getStatus()).isEqualTo("DNM");
        assertThat(record.getRoomNights()).isEqualTo(7);
    }

    @Test
    void toEntityShouldDeriveRoomNightsFromDatesWhenMissing() {
        ReservationArrivalDto source = new ReservationArrivalDto();
        source.setFirstName("John");
        source.setLastName("Smith");
        source.setConfirmationNumber("CNF-1001");
        source.setCheckInDate(LocalDate.of(2026, 6, 3));
        source.setCheckOutDate(LocalDate.of(2026, 6, 6));

        ArrivalRecord record = mapper.toEntity(source, "PROP001", LocalDate.of(2026, 6, 3));

        assertThat(record.getRoomNights()).isEqualTo(3);
    }

    @Test
    void toEntityShouldDefaultRoomNightsToOneWhenDatesMissing() {
        ReservationArrivalDto source = new ReservationArrivalDto();
        source.setFirstName("John");
        source.setLastName("Smith");
        source.setConfirmationNumber("CNF-1001");

        ArrivalRecord record = mapper.toEntity(source, "PROP001", LocalDate.of(2026, 6, 3));

        assertThat(record.getRoomNights()).isEqualTo(1);
    }

    @Test
    void toEntityShouldClampDerivedRoomNightsToOneWhenCheckOutIsNotAfterCheckIn() {
        ReservationArrivalDto source = new ReservationArrivalDto();
        source.setFirstName("John");
        source.setLastName("Smith");
        source.setConfirmationNumber("CNF-1001");
        source.setCheckInDate(LocalDate.of(2026, 6, 6));
        source.setCheckOutDate(LocalDate.of(2026, 6, 5));

        ArrivalRecord record = mapper.toEntity(source, "PROP001", LocalDate.of(2026, 6, 6));

        assertThat(record.getRoomNights()).isEqualTo(1);
    }

    @Test
    void updateEntityShouldMapAllMutableFields() {
        ArrivalRecord target = ArrivalRecord.builder().build();
        ReservationArrivalDto source = new ReservationArrivalDto();
        source.setStatus("DNM");
        source.setSalutation("Mr.");
        source.setFirstName("John");
        source.setLastName("Smith");
        source.setRoomNo("305");
        source.setReservationType("Guaranteed");
        source.setCity("Mumbai");
        source.setRateCode("BAR");
        source.setCheckInDate(LocalDate.of(2026, 6, 3));
        source.setCheckOutDate(LocalDate.of(2026, 6, 5));
        source.setRoomNights(2);
        source.setRoomStatus("Clean");
        source.setCorporateCode("CORP001");
        source.setRoomType("Deluxe King");
        source.setCompany("ABC Travels");
        source.setSharingStatus("Y");
        source.setFloor(3);
        source.setLoyaltyMembershipStatus("Gold Member");

        mapper.updateEntity(target, source);

        assertThat(target.getStatus()).isEqualTo("DNM");
        assertThat(target.getSalutation()).isEqualTo("Mr.");
        assertThat(target.getFirstName()).isEqualTo("John");
        assertThat(target.getLastName()).isEqualTo("Smith");
        assertThat(target.getRoomNo()).isEqualTo("305");
        assertThat(target.getReservationType()).isEqualTo("Guaranteed");
        assertThat(target.getCity()).isEqualTo("Mumbai");
        assertThat(target.getRateCode()).isEqualTo("BAR");
        assertThat(target.getCheckInDate()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(target.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(target.getRoomNights()).isEqualTo(2);
        assertThat(target.getRoomStatus()).isEqualTo("Clean");
        assertThat(target.getCorporateCode()).isEqualTo("CORP001");
        assertThat(target.getRoomType()).isEqualTo("Deluxe King");
        assertThat(target.getCompany()).isEqualTo("ABC Travels");
        assertThat(target.getSharingStatus()).isEqualTo("Y");
        assertThat(target.getFloor()).isEqualTo(3);
        assertThat(target.getLoyaltyMembershipStatus()).isEqualTo("Gold Member");
        assertThat(target.getSourceLastSyncedAt()).isNotNull();
    }

    @Test
    void toResponseShouldMapAllFields() {
        ArrivalRecord source = ArrivalRecord.builder()
                .id(1L)
                .propertyId("PROP001")
                .status("DNM")
                .salutation("Mr.")
                .firstName("John")
                .lastName("Smith")
                .roomNo("305")
                .reservationType("Guaranteed")
                .city("Mumbai")
                .rateCode("BAR")
                .checkInDate(LocalDate.of(2026, 6, 3))
                .checkOutDate(LocalDate.of(2026, 6, 5))
                .roomNights(2)
                .roomStatus("Clean")
                .corporateCode("CORP001")
                .roomType("Deluxe King")
                .confirmationNumber("CNF-1001")
                .company("ABC Travels")
                .sharingStatus("Y")
                .floor(3)
                .loyaltyMembershipStatus("Gold Member")
                .build();

        var response = mapper.toResponse(source);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPropertyId()).isEqualTo("PROP001");
        assertThat(response.getStatus()).isEqualTo("DNM");
        assertThat(response.getSalutation()).isEqualTo("Mr.");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getRoomNo()).isEqualTo("305");
        assertThat(response.getReservationType()).isEqualTo("Guaranteed");
        assertThat(response.getCity()).isEqualTo("Mumbai");
        assertThat(response.getRateCode()).isEqualTo("BAR");
        assertThat(response.getCheckInDate()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(response.getCheckOutDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(response.getRoomNights()).isEqualTo(2);
        assertThat(response.getRoomStatus()).isEqualTo("Clean");
        assertThat(response.getCorporateCode()).isEqualTo("CORP001");
        assertThat(response.getRoomType()).isEqualTo("Deluxe King");
        assertThat(response.getConfirmationNumber()).isEqualTo("CNF-1001");
        assertThat(response.getCompany()).isEqualTo("ABC Travels");
        assertThat(response.getSharingStatus()).isEqualTo("Y");
        assertThat(response.getFloor()).isEqualTo(3);
        assertThat(response.getLoyaltyMembershipStatus()).isEqualTo("Gold Member");
    }
}
