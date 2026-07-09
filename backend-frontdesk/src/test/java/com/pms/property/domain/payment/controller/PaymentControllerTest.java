package com.pms.property.domain.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.payment.dto.PaymentMethodResponse;
import com.pms.property.domain.payment.dto.PaymentSummaryResponse;
import com.pms.property.domain.payment.service.PaymentService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentControllerTest {

    @Test
    void shouldReturnSummaryPayload() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        when(service.getSummaryByPropertyId("P-1")).thenReturn(new PaymentSummaryResponse("P-1", 2));

        var response = controller.getSummary("P-1").getBody();

        assertEquals("P-1", response.data().propertyId());
    }

    @Test
    void shouldReturnMethodsPayload() {
        PaymentService service = mock(PaymentService.class);
        PaymentController controller = new PaymentController(service);
        when(service.listMethodsByPropertyId("P-1"))
            .thenReturn(List.of(new PaymentMethodResponse(1L, "P-1", "CARD", "CARD_GL", true, true)));

        var response = controller.listMethods("P-1").getBody();

        assertEquals(1, response.data().size());
    }
}

