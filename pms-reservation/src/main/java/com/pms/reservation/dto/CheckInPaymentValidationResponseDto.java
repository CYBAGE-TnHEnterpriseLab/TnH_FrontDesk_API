package com.pms.reservation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInPaymentValidationResponseDto {
    boolean passed;
    String message;
}
