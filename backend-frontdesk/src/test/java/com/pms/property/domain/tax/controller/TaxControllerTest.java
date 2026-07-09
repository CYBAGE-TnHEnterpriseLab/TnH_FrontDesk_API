package com.pms.property.domain.tax.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.tax.dto.TaxResponse;
import com.pms.property.domain.tax.dto.TaxSummaryResponse;
import com.pms.property.domain.tax.service.TaxService;
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
    void shouldReturnTaxConfigPayload() {
        TaxService service = mock(TaxService.class);
        TaxController controller = new TaxController(service);
        when(service.getTaxByPropertyId("P-1")).thenReturn(new TaxResponse(1L, "P-1", "GST123", 18.0));

        var response = controller.getCurrentByProperty("P-1").getBody();

        assertEquals("GST123", response.data().gstNumber());
    }
}

