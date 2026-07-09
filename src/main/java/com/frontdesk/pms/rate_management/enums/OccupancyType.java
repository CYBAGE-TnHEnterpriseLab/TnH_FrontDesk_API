package com.frontdesk.pms.rate_management.enums;

import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum OccupancyType {
    ONE_PERSON("1 Guest"),
    TWO_PERSON("2 Guest"),
    THREE_PERSON("3 Guest"),
    FOUR_PERSON("4 Guest"),
    EXTRA_ONE_PERSON("Extra Guest Charges(1P)"),
    EXTRA_TWO_PERSON("Extra Guest Charges(2P)");

    private final String label;

    OccupancyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        register(ONE_PERSON,
            "1 guest",
            "1g",
            "one guest",
            "one_guest",
            "oneguest");
        register(TWO_PERSON,
            "2 guest",
            "2g",
            "two guest",
            "two_guest",
            "two_guests",
            "twoguests");
        register(THREE_PERSON,
            "3 guest",
            "3g",
            "three guest",
            "three_guest",
            "three_guests",
            "threeguests");
        register(FOUR_PERSON,
            "4 guest",
            "4g",
            "four guest",
            "four_guest",
            "four_guests",
            "fourguests");
        register(EXTRA_TWO_PERSON,
                "extra guest charges(2p)",
                "extra guest charges (2p)",
                "extra guest charges",
                "extra 2p",
                "2 extra p",
            "extra two person",
            "extra_guest",
            "extra guest");
        register(EXTRA_ONE_PERSON, "extra 1p", "1 extra p", "extra one person", "extra_guest_1p");
    }

    public static String normalizeOrThrow(String rawOccupancyType) {
        if (rawOccupancyType == null || rawOccupancyType.isBlank()) {
            throw new InvalidRatePlanException("Occupancy type is required");
        }

        String key = normalizeKey(rawOccupancyType);
        String normalized = ALIASES.get(key);
        if (normalized != null) {
            return normalized;
        }

        throw new InvalidRatePlanException(
                "Invalid occupancy type: " + rawOccupancyType
                        + ". Allowed values include 1 Guest, 2 Guest, 3 Guest, 4 Guest, Extra Guest Charges(2P).");
    }

    private static void register(OccupancyType occupancyType, String... aliases) {
        ALIASES.put(normalizeKey(occupancyType.name()), occupancyType.getLabel());
        ALIASES.put(normalizeKey(occupancyType.getLabel()), occupancyType.getLabel());
        for (String alias : aliases) {
            ALIASES.put(normalizeKey(alias), occupancyType.getLabel());
        }
    }

    private static String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("(", "")
                .replace(")", "");
    }
}