package com.frontdesk.pms.room.exception;

public class FloorNotFoundException extends RuntimeException {

    public FloorNotFoundException(String message) {
        super(message);
    }
}