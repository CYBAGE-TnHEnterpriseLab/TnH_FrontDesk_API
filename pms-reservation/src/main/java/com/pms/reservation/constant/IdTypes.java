package com.pms.reservation.constant;

import java.util.List;

public final class IdTypes {

    public static final String AADHAAR = "AADHAAR";
    public static final String PAN = "PAN";
    public static final String DRIVING_LICENSE = "DRIVING_LICENSE";
    public static final String PASSPORT = "PASSPORT";
    public static final String FRONT_DESK = "FRONT_DESK";
    public static final String KIOSK = "KIOSK";
    public static final String ONLINE = "ONLINE";

    private IdTypes() {
    }

    public static List<String> supportedTypes() {
        return List.of(AADHAAR, PAN, DRIVING_LICENSE, PASSPORT);
    }

    public static boolean isSupported(String value) {
        return value != null && supportedTypes().stream().anyMatch(type -> type.equalsIgnoreCase(value.trim()));
    }

    public static boolean isSupportedCheckInChannel(String value) {
        return value != null && List.of(FRONT_DESK, KIOSK, ONLINE).stream()
                .anyMatch(channel -> channel.equalsIgnoreCase(value.trim()));
    }
}