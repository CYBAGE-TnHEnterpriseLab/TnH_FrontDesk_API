package com.frontdesk.pms.rate_management.enums;

public enum OccupancyType {
    ONE_PERSON("1P"),
    TWO_PERSON("2P"),
    THREE_PERSON("3P"),
    FOUR_PERSON("4P"),
    EXTRA_ONE_PERSON("Extra 1P"),
    EXTRA_TWO_PERSON("Extra 2P");

    private final String label;

    OccupancyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}