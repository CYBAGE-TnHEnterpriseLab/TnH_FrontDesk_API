package com.frontdesk.pms.rate_management.exception;

public class PropertyNotFoundException extends RuntimeException {
    public PropertyNotFoundException(String id) {
        super("Property not found with id: " + id);
    }
}
