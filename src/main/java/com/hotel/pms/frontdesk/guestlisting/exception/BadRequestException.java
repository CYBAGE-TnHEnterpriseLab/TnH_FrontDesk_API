package com.hotel.pms.frontdesk.guestlisting.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
