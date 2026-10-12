package com.pms.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MakeReservationGuestPrimaryRequest {

    @NotNull(message = "bookingId is required")
    @Positive(message = "bookingId must be positive")
    private Long bookingId;
}
