package com.folio.billing.dto;

import java.time.LocalDate;

public record FolioBillingFilter(
        String roomNumber,
        String guestName,
        String actnerCrop,
        String confirmationNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}
