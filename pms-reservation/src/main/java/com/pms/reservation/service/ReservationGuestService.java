package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationGuestResponseDto;
import java.util.List;

public interface ReservationGuestService {

    ReservationGuestResponseDto assignGuestToBooking(Long bookingId, Long guestProfileId, boolean primary);

    List<ReservationGuestResponseDto> getGuestsForBooking(Long bookingId);

    ReservationGuestResponseDto getPrimaryGuest(Long bookingId);

    ReservationGuestResponseDto makePrimaryGuest(Long bookingId, Long reservationGuestId);

    void removeGuestFromBooking(Long bookingId, Long guestProfileId);
}
