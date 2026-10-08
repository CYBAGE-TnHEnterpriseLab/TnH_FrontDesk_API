package com.pms.reservation.dto;

import com.pms.reservation.integration.dto.GuestProfileResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationGuestResponseDto {
    Long id;
    Long bookingId;
    Long guestProfileId;
    Boolean isPrimary;
    GuestProfileResponse guestProfile;
}
