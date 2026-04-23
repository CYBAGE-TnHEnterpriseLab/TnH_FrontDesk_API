package com.frontdesk.pms.exception;

public class PropertyAlreadyExistsException extends RuntimeException {

    public PropertyAlreadyExistsException(String message) {
        super(message);
    }
}