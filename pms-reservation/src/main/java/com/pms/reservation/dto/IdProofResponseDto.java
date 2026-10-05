package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IdProofResponseDto {
    Long bookingId;
    String confirmationNumber;
    String propertyId;
    String idProofType;
    String idProofNumber;
    String contentType;
    String checkInChannel;
    @JsonIgnore
    String payloadBase64;
    LocalDateTime uploadedAt;
}