package com.folio.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FolioDocumentGenerateRequest(
        @NotBlank(message = "confirmationNo is required")
        String confirmationNo,
        @NotNull(message = "documentType is required")
        FolioDocumentType documentType,
        String userId
) {
}
