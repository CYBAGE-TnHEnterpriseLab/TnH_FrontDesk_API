package com.pms.housekeeping.common.exception;

public class HousekeepingException extends RuntimeException {

    public HousekeepingException(String message) {
        super(message);
    }

    public HousekeepingException(String message, Throwable cause) {
        super(message, cause);
    }
}

