package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillingDetailsResponse(
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance,
        String guestName,
        String primaryGuest,
        String secondaryGuest,
        String confirmationNo,
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
