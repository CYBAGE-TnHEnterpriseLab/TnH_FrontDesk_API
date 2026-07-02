package com.pms.property.domain.payment.dto;

public record PaymentMethodRequest(
    String paymentMethod,
    String accountMapping,
    Boolean allowRefund,
    Boolean active
) {
}

