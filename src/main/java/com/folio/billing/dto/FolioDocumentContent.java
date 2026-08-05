package com.folio.billing.dto;

import java.time.LocalDateTime;

public record FolioDocumentContent(
        String documentId,
        String confirmationNo,
        FolioDocumentType documentType,
        String fileName,
        String contentType,
        String content,
        LocalDateTime generatedAt,
        String generatedBy
) {
}
