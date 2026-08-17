package com.pms.reservation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutCompletionResponseDto {
    Long bookingId;
    String confirmationNumber;
    String reservationStatus;
    LocalDate businessDate;
    LocalDateTime checkOutCompletedAt;
    String checkOutCompletedBy;
}
