package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BillingDetailsResponse(
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance,
        List<String> folios,
        String activeFolioCode,
        String guestName,
        String primaryGuest,
        String secondaryGuest,
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
        BillingComments comments
) {
}

