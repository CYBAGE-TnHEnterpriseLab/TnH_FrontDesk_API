package com.pms.housekeeping.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class HousekeepingException extends RuntimeException {

    public HousekeepingException(String message) {
        super(message);
    }

    public HousekeepingException(String message, Throwable cause) {
        super(message, cause);
    }
}

