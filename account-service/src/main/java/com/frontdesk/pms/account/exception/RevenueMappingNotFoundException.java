package com.frontdesk.pms.account.exception;

import java.util.UUID;

public class RevenueMappingNotFoundException extends RuntimeException {
    public RevenueMappingNotFoundException(UUID mappingId) {
        super("Revenue mapping not found: " + mappingId);
    }
}
