package com.frontdesk.pms.room.exception;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(String message) {
        super(message);
    }
}