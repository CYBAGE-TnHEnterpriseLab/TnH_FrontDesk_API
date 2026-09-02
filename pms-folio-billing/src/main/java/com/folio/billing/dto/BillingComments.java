package com.folio.billing.dto;

import java.util.List;

public record BillingComments(
        List<String> guestRequests,
        String billingComments
) {
}