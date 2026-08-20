package com.folio.billing.dto;

import java.util.List;
import java.util.Map;

public record FolioChargeOptionsResponse(
        List<String> transactionTypes,
        Map<String, List<String>> categoriesByTransactionType
) {
}
