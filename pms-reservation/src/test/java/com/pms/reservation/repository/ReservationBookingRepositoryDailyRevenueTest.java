package com.pms.reservation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.reservation.entity.ReservationBookingRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Room charges for the dashboard: the one-night charge of every room arriving on the business
 * date, counted while the arrival is expected or checked in, and gone once it has checked out.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationBookingRepositoryDailyRevenueTest {

    private static final String PROPERTY_ID = "7cfd4559-b6f3-4b7d-b933-e93018ac1d47";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 12);

    @Autowired
    private ReservationBookingRepository repository;

    @Test
    void arrivalsOfTheDayContributeOneNightPerRoom() {
        saveBooking("CONF-1", "CONFIRMED", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "900.00", 1);
        saveBooking("CONF-2", "CHECKED_IN", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "1200.00", 1);

        DailyRevenueProjection revenue = findDailyRevenue();

        assertThat(revenue.getRoomRevenue()).isEqualByComparingTo("2100.00");
        assertThat(revenue.getRoomsSold()).isEqualTo(2L);
    }

    @Test
    void checkedInArrivalStaysCountedAndCheckedOutArrivalDisappears() {
        saveBooking("CONF-3", "CHECKED_IN", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "900.00", 1);

        assertThat(findDailyRevenue().getRoomRevenue()).isEqualByComparingTo("900.00");

        saveBooking("CONF-4", "CHECKED_OUT", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "1200.00", 1);

        DailyRevenueProjection afterCheckout = findDailyRevenue();
        assertThat(afterCheckout.getRoomRevenue()).isEqualByComparingTo("900.00");
        assertThat(afterCheckout.getRoomsSold()).isEqualTo(1L);
    }

    @Test
    void cancelledAndNoShowArrivalsAreExcluded() {
        saveBooking("CONF-5", "CANCELLED", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "900.00", 1);
        saveBooking("CONF-6", "NO_SHOW", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "900.00", 1);

        assertThat(findDailyRevenue().getRoomRevenue()).isEqualByComparingTo("0.00");
    }

    @Test
    void arrivalsOnOtherDatesAreExcluded() {
        saveBooking("CONF-7", "CONFIRMED", LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 16), "900.00", 1);
        saveBooking("CONF-8", "CHECKED_IN", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16), "900.00", 1);

        assertThat(findDailyRevenue().getRoomRevenue()).isEqualByComparingTo("0.00");
    }

    @Test
    void sameDayArrivalCountsItsOneNight() {
        saveBooking("CONF-9", "CHECKED_IN", BUSINESS_DATE, BUSINESS_DATE, "900.00", 1);

        assertThat(findDailyRevenue().getRoomRevenue()).isEqualByComparingTo("900.00");
    }

    @Test
    void multiRoomArrivalCountsEveryRoom() {
        saveBooking("CONF-10", "CHECKED_IN", BUSINESS_DATE, LocalDate.of(2026, 10, 16), "900.00", 3);

        assertThat(findDailyRevenue().getRoomRevenue()).isEqualByComparingTo("2700.00");
        assertThat(findDailyRevenue().getRoomsSold()).isEqualTo(3L);
    }

    private DailyRevenueProjection findDailyRevenue() {
        Optional<DailyRevenueProjection> revenue = repository.findDailyRevenue(PROPERTY_ID, BUSINESS_DATE);
        assertThat(revenue).isPresent();
        return revenue.get();
    }

    private void saveBooking(
            String confirmationNumber,
            String status,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String rate,
            int rooms
    ) {
        BigDecimal nightlyRate = new BigDecimal(rate);
        repository.save(ReservationBookingRecord.builder()
                .confirmationNumber(confirmationNumber)
                .reservationStatus(status)
                .propertyId(PROPERTY_ID)
                .salutation("Mr")
                .vipTag(false)
                .guestName("Guest " + confirmationNumber)
                .enrollGuest(false)
                .guestNamesEncoded("")
                .personalEmail("guest@example.com")
                .officialEmail("guest@work.example")
                .city("Pune")
                .country("India")
                .zipCode("411001")
                .phoneNumber("9876543210")
                .mobileNumber("9876543210")
                .arrivalDate(arrivalDate)
                .departureDate(departureDate)
                .adultCount(2)
                .childCount(0)
                .reservationType("INDIVIDUAL")
                .roomType("Deluxe Twin")
                .assignedRoomNo("117")
                .rateCode("BAR")
                .numberOfRooms(rooms)
                .rate(nightlyRate)
                .totalRate(nightlyRate.multiply(BigDecimal.valueOf(rooms)))
                .payment("CASH")
                .paymentType("ADVANCE")
                .eta(LocalTime.of(15, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .dnm(false)
                .noPost(false)
                .guestBalance(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .build());
        repository.flush();
    }
}
