package com.folio.billing.dto;

import java.math.BigDecimal;

public record FolioTransactionAmountUpdateResponse(
        String confirmationNumber,
        String folioId,
        FolioChargePostResponse.Transaction transaction,
        BigDecimal totalCharges,
        BigDecimal totalCredits,
        BigDecimal balance
) {}
