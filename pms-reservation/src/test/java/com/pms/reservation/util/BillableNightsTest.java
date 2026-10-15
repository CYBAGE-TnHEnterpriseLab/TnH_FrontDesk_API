package com.pms.reservation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillableNightsTest {

    @Test
    void sameDayStayIsBilledAsOneNight() {
        LocalDate stayDate = LocalDate.of(2026, 10, 12);

        assertEquals(1, BillableNights.between(stayDate, stayDate));
        assertEquals(1, BillableNights.betweenAsInt(stayDate, stayDate));
    }

    @Test
    void multiNightStayIsBilledPerNight() {
        assertEquals(1, BillableNights.between(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)));
        assertEquals(4, BillableNights.between(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16)));
    }

    @Test
    void missingDatesHaveNoBillableNights() {
        assertEquals(0, BillableNights.between(null, LocalDate.of(2026, 10, 13)));
        assertEquals(0, BillableNights.between(LocalDate.of(2026, 10, 12), null));
    }
}
