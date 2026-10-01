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
        String confirmationNumber,
        Long bookingId
) {
    public FolioBillingRow(String tier, String lastName, String firstName, String room, String guest,
                           String stayStatus, LocalDate checkIn, LocalDate checkOut, int nights,
                           String houseKeeping, String roomType, String confirmationNumber) {
        this(tier, lastName, firstName, room, guest, stayStatus, checkIn, checkOut, nights,
                houseKeeping, roomType, confirmationNumber, null);
    }
}

