package Policy_Management.Policy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    ACTIVE,
    INACTIVE,
    PENDING,
    EXPIRED,
    DRAFT,
    PUBLISHED;

    @JsonCreator
    public static Status fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @JsonValue
    public String getValue() {
        return name();
    }
}
