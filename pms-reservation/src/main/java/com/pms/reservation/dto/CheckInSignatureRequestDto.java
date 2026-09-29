package com.pms.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInSignatureRequestDto {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotBlank(message = "confirmationNumber is required")
    private String confirmationNumber;

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotBlank(message = "signature payload is required")
    private String payloadBase64;
}
