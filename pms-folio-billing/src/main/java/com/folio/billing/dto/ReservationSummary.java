package com.folio.billing.dto;

import java.math.BigDecimal;
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
        BillingComments comments,
        BigDecimal reservationAmount,
        Long bookingId
) {
    public ReservationSummary(String guestName, String guest1, String guest2, String confirmationNumber,
                              int adults, int children, String company, String bookingSource, String ratePlan,
                              String reservationStatus, String folioStatus, String roomNo, String roomType,
                              LocalDate checkInDate, LocalDate checkOutDate, int nights, BillingComments comments,
                              BigDecimal reservationAmount) {
        this(guestName, guest1, guest2, confirmationNumber, adults, children, company, bookingSource, ratePlan,
                reservationStatus, folioStatus, roomNo, roomType, checkInDate, checkOutDate, nights, comments,
                reservationAmount, null);
    }
}

