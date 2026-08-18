package com.pms.reservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInSignatureResponseDto {
    Long bookingId;
    String confirmationNumber;
    String contentType;
    String payloadBase64;
    LocalDateTime signedAt;
}
