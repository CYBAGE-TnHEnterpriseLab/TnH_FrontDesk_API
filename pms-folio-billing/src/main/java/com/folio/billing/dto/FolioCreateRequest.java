package com.folio.billing.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record FolioCreateRequest(
        @JsonAlias("confirmationNo")
        @NotBlank(message = "confirmationNumber is required")
        String confirmationNumber,
                Long bookingId,
        String roomNo,
        String guestName,
        String userId
) {
        public FolioCreateRequest(String confirmationNumber, String roomNo, String guestName, String userId) {
                this(confirmationNumber, null, roomNo, guestName, userId);
        }
}

