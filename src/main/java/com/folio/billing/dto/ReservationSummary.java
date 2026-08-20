package com.folio.billing.dto;

import java.time.LocalDate;

public record ReservationSummary(
        String guestName,
        String guest1,
        String guest2,
        String confirmationNumber,
        int adults,
        int children,
        String company,
        String bookingSource,
        String ratePlan,
        String reservationStatus,
        String folioStatus,
        String roomNo,
        String roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        String comments
) {
}

