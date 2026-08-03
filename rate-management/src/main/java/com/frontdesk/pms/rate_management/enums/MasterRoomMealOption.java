package com.frontdesk.pms.rate_management.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MasterRoomMealOption {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    ALL_MEALS("all meals"),
    BREAKFAST_AND_LUNCH("breakfast and lunch");

    private final String label;

    MasterRoomMealOption(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static MasterRoomMealOption fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim().toLowerCase().replace('_', ' ').replace('-', ' ');
        normalized = normalized.replaceAll("\\s+", " ");

        // Accept common UI combined-value formats like "Breakfast, Lunch" or "Breakfast & Lunch".
        String normalizedValue = normalized.replace("&", " and ")
                .replace("+", " and ")
                .replace(",", " and ")
                .replaceAll("\\s+", " ")
                .trim();

        String resolvedValue = "lunch and breakfast".equals(normalizedValue)
                ? "breakfast and lunch"
                : normalizedValue;

        return Arrays.stream(values())
            .filter(option -> option.label.equals(resolvedValue)
                        || option.name().equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid meal option: " + rawValue + ". Allowed values: breakfast, lunch, dinner, all meals, breakfast and lunch"
                ));
    }
}