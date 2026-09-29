package com.pms.reservation.dto;

import com.pms.reservation.constant.IdTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdProofRequestDto {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotBlank(message = "confirmationNumber is required")
    private String confirmationNumber;

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    private String checkInChannel;

    @NotBlank(message = "idProofType is required")
        @Schema(description = "Guest proof of identity type", allowableValues = {
            IdTypes.AADHAAR,
            IdTypes.PAN,
            IdTypes.DRIVING_LICENSE,
            IdTypes.PASSPORT
        })
        @Pattern(regexp = "(?i)AADHAAR|PAN|DRIVING_LICENSE|PASSPORT",
            message = "idProofType must be AADHAAR, PAN, DRIVING_LICENSE, or PASSPORT")
    private String idProofType;

    @NotBlank(message = "idProofNumber is required")
    private String idProofNumber;

    @NotBlank(message = "contentType is required")
    private String contentType;

    @NotBlank(message = "ID proof payload is required")
    private String payloadBase64;
}