package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.dto.HousekeepingSyncResponse;
import java.math.BigDecimal;
import java.util.List;

public interface ReservationBookingService {

    ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request);

    ReservationViewResponseDto updateBooking(String confirmationNumber, ReservationBookingRequestDto request);

    ReservationViewResponseDto updateBooking(String confirmationNumber, Long bookingId, ReservationBookingRequestDto request);

    ReservationViewResponseDto getBookingDetails(String confirmationNumber);

        ReservationViewResponseDto searchBooking(
            String confirmationNumber,
            Long bookingId,
            String phoneNumber,
            String email);

    ReservationViewResponseDto getBookingDetails(String confirmationNumber, Long bookingId);

    void updateGuestBalance(String confirmationNumber, Long bookingId, BigDecimal guestBalance);

    List<ReservationBookingResponseDto> getBookings();

    HousekeepingSyncResponse syncHousekeepingStatuses();
}
