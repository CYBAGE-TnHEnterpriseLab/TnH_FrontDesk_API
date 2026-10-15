package com.pms.guest.exception;

public class ReservationServiceException extends RuntimeException {

    public ReservationServiceException(String message) {
        super(message);
    }

    public ReservationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
