package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;

public interface ReservationBookingService {

    ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request);
}
