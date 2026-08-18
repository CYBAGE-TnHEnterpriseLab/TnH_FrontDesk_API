package com.pms.reservation.service;

import com.pms.reservation.dto.ReservationAvailabilityRequestDto;
import com.pms.reservation.dto.ReservationAvailabilityResponseDto;

public interface ReservationAvailabilityService {

    ReservationAvailabilityResponseDto getAvailability(ReservationAvailabilityRequestDto request);
}
