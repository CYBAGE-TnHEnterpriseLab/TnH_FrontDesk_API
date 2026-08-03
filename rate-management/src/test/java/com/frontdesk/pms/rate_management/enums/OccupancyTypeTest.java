package com.frontdesk.pms.rate_management.enums;

import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OccupancyTypeTest {

    @Test
    void normalizeOrThrow_shouldAcceptFrontendConstants() {
        assertEquals("1 Guest", OccupancyType.normalizeOrThrow("ONE_GUEST"));
        assertEquals("2 Guest", OccupancyType.normalizeOrThrow("TWO_GUESTS"));
        assertEquals("3 Guest", OccupancyType.normalizeOrThrow("THREE_GUESTS"));
        assertEquals("4 Guest", OccupancyType.normalizeOrThrow("FOUR_GUESTS"));
        assertEquals("Extra Guest Charges(2P)", OccupancyType.normalizeOrThrow("EXTRA_GUEST"));
    }

    @Test
    void normalizeOrThrow_shouldRejectUnknownValue() {
        assertThrows(InvalidRatePlanException.class, () -> OccupancyType.normalizeOrThrow("UNKNOWN_OCCUPANCY"));
    }
}
