package com.pms.reservation.constant;

import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class PaymentModes {

    public static final String CARD = "CARD";
    public static final String CASH = "CASH";
    public static final String UPI = "UPI";
    public static final String NET_BANKING = "NET_BANKING";
    public static final String WALLET = "WALLET";

    public static final String VALIDATION_PATTERN = "(?i)CARD|CASH|UPI|NET[\\s_-]?BANKING|WALLET";

    private static final List<String> SUPPORTED_PAYMENT_MODES = List.of(
            CARD,
            CASH,
            UPI,
            NET_BANKING,
            WALLET
    );

    private PaymentModes() {
    }

    public static List<String> supportedModes() {
        return SUPPORTED_PAYMENT_MODES;
    }

    public static String normalize(String paymentMode) {
        if (!StringUtils.hasText(paymentMode)) {
            return paymentMode;
        }

        return paymentMode
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    public static boolean isSupported(String normalizedPaymentMode) {
        return SUPPORTED_PAYMENT_MODES.contains(normalizedPaymentMode);
    }
}