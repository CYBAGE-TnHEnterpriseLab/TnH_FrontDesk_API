package com.frontdesk.pms.rate_management.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontdesk.pms.rate_management.dto.RatePlanRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatePlanCalculationMethodTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromValue_shouldAcceptEnumConstantsAndUiLabels() {
        assertEquals(RatePlanCalculationMethod.PERCENT_OFF_BAR,
                RatePlanCalculationMethod.fromValue("PERCENT_OFF_BAR"));
        assertEquals(RatePlanCalculationMethod.PERCENT_ADD_BAR,
                RatePlanCalculationMethod.fromValue("% Add to BAR"));
        assertEquals(RatePlanCalculationMethod.FLAT_OFF_BAR,
                RatePlanCalculationMethod.fromValue("Flat Amount Off BAR"));
        assertEquals(RatePlanCalculationMethod.FLAT_ADD_BAR,
                RatePlanCalculationMethod.fromValue("Flat Amount Add to BAR"));
        assertEquals(RatePlanCalculationMethod.MANUAL,
                RatePlanCalculationMethod.fromValue("manual"));
    }

    @Test
    void fromValue_shouldRejectUnknownValue() {
        assertThrows(IllegalArgumentException.class,
                () -> RatePlanCalculationMethod.fromValue("UNKNOWN_METHOD"));
    }

    @Test
    void deserializeRatePlanRequest_shouldAcceptUiLabel() throws Exception {
        String json = "{\"calculationMethod\":\"Flat Amount Off BAR\"}";

        RatePlanRequestDTO request = objectMapper.readValue(json, RatePlanRequestDTO.class);

        assertEquals(RatePlanCalculationMethod.FLAT_OFF_BAR, request.getCalculationMethod());
    }
}
