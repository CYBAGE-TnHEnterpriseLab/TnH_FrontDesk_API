package com.pms.guest.service;

import com.pms.guest.dto.response.GuestDetailsResponse;

public interface GuestDetailsService {

    /**
     * Read-only, property-scoped guest-details retrieval. Exactly one of phoneNumber, email,
     * bookingId or confirmationNumber must be supplied. Returns every matching guest-booking assignment.
     */
    GuestDetailsResponse getGuestDetails(
            String propertyId,
            String phoneNumber,
            String email,
            Long bookingId,
            String confirmationNumber
    );
}
