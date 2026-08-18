package com.folio.billing.dto;

import java.time.LocalDate;

public record FolioBillingRow(
        String tier,
        String lastName,
        String firstName,
        String room,
        String guest,
        String stayStatus,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        String houseKeeping,
        String roomType,
        String confirmationNo
) {
}
