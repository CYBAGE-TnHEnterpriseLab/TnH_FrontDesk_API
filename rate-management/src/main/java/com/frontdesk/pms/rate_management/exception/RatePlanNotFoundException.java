package com.frontdesk.pms.rate_management.exception;

public class RatePlanNotFoundException extends RuntimeException {
    public RatePlanNotFoundException(Long id) {
        super("Rate plan not found for id: " + id);
    }
}
