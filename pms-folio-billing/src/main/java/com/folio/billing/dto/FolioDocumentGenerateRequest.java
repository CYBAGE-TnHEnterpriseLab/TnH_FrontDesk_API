package com.folio.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FolioDocumentGenerateRequest(
        @NotBlank(message = "confirmationNumber is required")
        String confirmationNumber,
        @NotNull(message = "documentType is required")
        FolioDocumentType documentType,
        String userId
) {
}

