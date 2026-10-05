package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationGuestAssignmentDto;
import com.pms.reservation.dto.ReservationGuestResponseDto;
import java.util.List;

public interface ReservationGuestService {

    void assignResolvedGuests(Long bookingId, List<ResolvedReservationGuest> guests);

    ReservationGuestResponseDto assignGuestToBooking(Long bookingId, Long guestProfileId, boolean primary);

    List<ReservationGuestResponseDto> getGuestsForBooking(Long bookingId);

    ReservationGuestResponseDto getPrimaryGuest(Long bookingId);

    ReservationGuestResponseDto makePrimaryGuest(Long bookingId, Long reservationGuestId);

    void removeGuestFromBooking(Long bookingId, Long guestProfileId);

    /**
     * Returns property-scoped guest assignments for exactly one of bookingId, confirmationNumber
     * or guestProfileIds. Guest profile details are intentionally not resolved here.
     */
    List<ReservationGuestAssignmentDto> findGuestAssignments(
            String propertyId,
            Long bookingId,
            String confirmationNumber,
            List<Long> guestProfileIds
    );
}
