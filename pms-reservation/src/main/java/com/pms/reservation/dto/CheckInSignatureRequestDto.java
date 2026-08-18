package com.pms.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInSignatureRequestDto {

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotBlank(message = "signature payload is required")
    private String payloadBase64;
}
