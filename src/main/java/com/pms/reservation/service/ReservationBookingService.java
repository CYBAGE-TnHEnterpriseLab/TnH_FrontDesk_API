package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import java.util.List;

public interface ReservationBookingService {

    ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request);

    ReservationViewResponseDto updateBooking(String confirmationNumber, ReservationBookingRequestDto request);

    ReservationViewResponseDto getBookingDetails(String confirmationNumber);

    List<ReservationBookingResponseDto> getBookings();
}
