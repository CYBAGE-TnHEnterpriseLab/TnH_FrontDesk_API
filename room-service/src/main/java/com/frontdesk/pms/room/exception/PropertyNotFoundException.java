package com.frontdesk.pms.room.exception;

import java.util.UUID;

public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException(UUID propertyId) {
        super("Property not found: " + propertyId);
    }
}
