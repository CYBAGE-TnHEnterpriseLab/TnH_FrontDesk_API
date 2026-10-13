package com.pms.loyalty.exception;

/**
 * Thrown when a loyalty number cannot be matched to any loyalty membership.
 */
public class LoyaltyNumberNotFoundException extends RuntimeException {

    public LoyaltyNumberNotFoundException(String loyaltyNumber) {
        super("loyalty number invalid and no guest profile found for the given loyalty number: " + loyaltyNumber);
    }
}
