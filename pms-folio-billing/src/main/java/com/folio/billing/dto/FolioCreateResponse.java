package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FolioCreateResponse(
        String confirmationNumber,
        String guestName,
        String roomNo,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance,
        Instant createdAt,
        Instant lastUpdatedAt,
        String userId
) {
}

