package com.pms.property.domain.tax.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.tax.dto.TaxRuleResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.service.TaxService;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaxControllerTest {

    @Test
    void shouldReturnSummaryPayload() {
        TaxService service = mock(TaxService.class);
        TaxController controller = new TaxController(service);
        when(service.getSummaryByPropertyId("P-1")).thenReturn(new TaxSummaryResponse("P-1", true, 2));

        var response = controller.getSummary("P-1").getBody();

        assertEquals("P-1", response.data().propertyId());
    }

    @Test
    void shouldReturnRulesPayload() {
        TaxService service = mock(TaxService.class);
        TaxController controller = new TaxController(service);
        when(service.getTaxRulesByPropertyId("P-1")).thenReturn(
            List.of(new TaxRuleResponse(1L, "P-1", "Service Tax", "Percentage", 6.0, "Add On", "Exclusive", "2026-05-12", true, "ACTIVE", 1))
        );

        var response = controller.getRules("P-1").getBody();

        assertEquals(1, response.data().size());
        assertEquals("Service Tax", response.data().get(0).taxName());
    }
}
