package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.dto.HousekeepingSyncResponse;
import java.util.List;

public interface ReservationBookingService {

    ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request);

    ReservationViewResponseDto updateBooking(String confirmationNumber, ReservationBookingRequestDto request);

    ReservationViewResponseDto updateBooking(String confirmationNumber, Long bookingId, ReservationBookingRequestDto request);

    ReservationViewResponseDto getBookingDetails(String confirmationNumber);

    ReservationViewResponseDto getBookingDetails(String confirmationNumber, Long bookingId);

    List<ReservationBookingResponseDto> getBookings();

    HousekeepingSyncResponse syncHousekeepingStatuses();
}
