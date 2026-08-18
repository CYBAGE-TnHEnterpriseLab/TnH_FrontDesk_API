package com.pms.reservation.constant;

import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class PaymentTypes {

    public static final String ADVANCE = "ADVANCE";
    public static final String FULL_PAYMENT = "FULL_PAYMENT";

    public static final String VALIDATION_PATTERN = "(?i)ADVANCE|FULL[\\s_-]?PAYMENT";

    private static final List<String> SUPPORTED_PAYMENT_TYPES = List.of(
        ADVANCE,
        FULL_PAYMENT
    );

    private PaymentTypes() {
    }

    public static List<String> supportedTypes() {
        return SUPPORTED_PAYMENT_TYPES;
    }

    public static String normalize(String paymentType) {
        if (!StringUtils.hasText(paymentType)) {
            return paymentType;
        }

        return paymentType
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
    }

    public static boolean isSupported(String normalizedPaymentType) {
        return SUPPORTED_PAYMENT_TYPES.contains(normalizedPaymentType);
    }
}
