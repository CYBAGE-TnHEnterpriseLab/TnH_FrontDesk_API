package com.frontdesk.pms.rate_management.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum RatePlanCalculationMethod {
    MANUAL,
    // Percentage discount on BAR from master pricing.
    PERCENT_OFF_BAR,
    // Percentage markup on BAR from master pricing.
    PERCENT_ADD_BAR,
    // Flat discount on BAR from master pricing.
    FLAT_OFF_BAR,
    // Flat markup on BAR from master pricing.
    FLAT_ADD_BAR;

    private static final Map<String, RatePlanCalculationMethod> ALIASES = new HashMap<>();

    static {
        register(MANUAL,
                "manual",
                "manual amount",
                "manual price");
        register(PERCENT_OFF_BAR,
                "percent off bar",
                "% off bar",
                "% of bar",
                "percentage off bar",
                "percent off from bar");
        register(PERCENT_ADD_BAR,
                "percent add bar",
                "% add bar",
                "% add to bar",
                "percentage add bar",
                "percent add to bar");
        register(FLAT_OFF_BAR,
                "flat off bar",
                "flat amount off bar",
                "flat discount bar");
        register(FLAT_ADD_BAR,
                "flat add bar",
                "flat amount add bar",
                "flat amount add to bar",
                "flat markup bar");
    }

    @JsonCreator
    public static RatePlanCalculationMethod fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        RatePlanCalculationMethod method = ALIASES.get(normalizeKey(rawValue));
        if (method != null) {
            return method;
        }

        throw new IllegalArgumentException(
                "Invalid calculation method: " + rawValue
                        + ". Allowed values include MANUAL, PERCENT_OFF_BAR, PERCENT_ADD_BAR, FLAT_OFF_BAR, FLAT_ADD_BAR."
        );
    }

    private static void register(RatePlanCalculationMethod method, String... aliases) {
        ALIASES.put(normalizeKey(method.name()), method);
        for (String alias : aliases) {
            ALIASES.put(normalizeKey(alias), method);
        }
    }

    private static String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("%", "percent")
                .replace("(", "")
                .replace(")", "");
    }
}
