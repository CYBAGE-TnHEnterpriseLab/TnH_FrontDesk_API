package com.pms.reservation.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckInSignatureResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    String contentType;
    @JsonIgnore
    String payloadBase64;
    String checkInChannel;
    LocalDateTime signedAt;
}
