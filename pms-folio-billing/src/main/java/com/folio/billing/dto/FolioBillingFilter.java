package com.folio.billing.dto;

import java.time.LocalDate;

public record FolioBillingFilter(
        String roomNumber,
        String guestName,
        String company,
        String confirmationNumber,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String propertyId,
        Long bookingId
) {
    public FolioBillingFilter(
            String roomNumber,
            String guestName,
            String company,
            String confirmationNumber,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        this(roomNumber, guestName, company, confirmationNumber, checkInDate, checkOutDate, null, null);
    }

    public FolioBillingFilter(String roomNumber, String guestName, String company, String confirmationNumber,
                              LocalDate checkInDate, LocalDate checkOutDate, String propertyId) {
        this(roomNumber, guestName, company, confirmationNumber, checkInDate, checkOutDate, propertyId, null);
    }
}
