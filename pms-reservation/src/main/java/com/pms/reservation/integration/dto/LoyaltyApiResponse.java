package com.pms.reservation.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

/**
 * Envelope every pms-loyalty endpoint wraps its payload in.
 *
 * The loyalty membership responses must be read through this wrapper: deserializing the body straight into
 * LoyaltyMembershipResponse silently drops payload, because the number and tier live under "data" and every
 * other field is absent, leaving them null with no error raised.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoyaltyApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Object errors;
    private OffsetDateTime timestamp;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Object getErrors() {
        return errors;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
