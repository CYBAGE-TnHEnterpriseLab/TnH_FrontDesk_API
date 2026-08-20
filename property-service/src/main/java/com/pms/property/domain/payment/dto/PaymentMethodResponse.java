package com.pms.property.domain.payment.dto;

public record PaymentMethodResponse(
    Long id,
    String propertyId,
    String paymentMethod,
    String accountMapping,
    Boolean allowRefund,
    Boolean active
) {
}

