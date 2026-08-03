package com.pms.housekeeping.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HousekeepingNotFoundException extends HousekeepingException {

    public HousekeepingNotFoundException(String message) {
        super(message);
    }
}

