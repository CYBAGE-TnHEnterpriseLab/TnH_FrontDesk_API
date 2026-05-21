package com.frontdesk.pms.rate_management.enums;

public enum RatePlanCalculationMethod {
    MANUAL,
    // Percentage discount on BAR from master pricing.
    PERCENT_OFF_BAR,
    // Percentage markup on BAR from master pricing.
    PERCENT_ADD_BAR,
    // Flat discount on BAR from master pricing.
    FLAT_OFF_BAR,
    // Flat markup on BAR from master pricing.
    FLAT_ADD_BAR
}
