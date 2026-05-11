package com.frontdesk.pms.account.enums;

public enum ChargeType {
    ROOM_CHARGES("Revenue from room bookings"),
    ADD_ONS("Revenue from extra services"),
    TAXES("Tax liability on charges"),
    CANCELLATION_FEES("Fees charged on cancellations"),
    NO_SHOW_FEES("Fees for guest no-shows");

    private final String description;

    ChargeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
