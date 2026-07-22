package com.pms.property.domain.payment.dto;

public record PaymentSummaryResponse(
    String propertyId,
    long paymentMethodsCount
) {
}

