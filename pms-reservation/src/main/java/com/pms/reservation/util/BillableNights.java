package com.pms.reservation.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Nights a stay is charged for. A same-day stay, meaning the guest arrives and departs on the same
 * date, is billed as one night; every other stay is billed for the nights between the dates.
 */
public final class BillableNights {

    private BillableNights() {
    }

    public static long between(LocalDate arrivalDate, LocalDate departureDate) {
        if (arrivalDate == null || departureDate == null) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(arrivalDate, departureDate);
        if (nights <= 0) {
            return 1;
        }
        return nights;
    }

    public static int betweenAsInt(LocalDate arrivalDate, LocalDate departureDate) {
        return Math.toIntExact(between(arrivalDate, departureDate));
    }
}
