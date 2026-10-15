package com.pms.reservation.service;

/**
 * A guest profile resolved for a reservation.
 *
 * {@code loyaltyNumber} is the membership number the profile carries after enrollment, so the booking
 * can report the number assigned during this reservation. {@code null} when the guest is not a member.
 */
public record ResolvedReservationGuest(Long guestProfileId, Boolean isPrimary, String loyaltyNumber) {

    public ResolvedReservationGuest(Long guestProfileId, Boolean isPrimary) {
        this(guestProfileId, isPrimary, null);
    }
}
