package com.pms.property.domain.finance.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pms.property.domain.finance.dto.ChartOfAccountResponse;
import com.pms.property.domain.finance.dto.FinanceSummaryResponse;
import com.pms.property.domain.finance.service.FinanceService;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceControllerTest {

    @Test
    void shouldReturnSummaryPayload() {
        FinanceService service = mock(FinanceService.class);
        FinanceController controller = new FinanceController(service);
        when(service.getSummaryByPropertyId("P-1")).thenReturn(new FinanceSummaryResponse("P-1", 2, 1));

        var response = controller.getSummary("P-1").getBody();

        assertEquals("P-1", response.data().propertyId());
    }

    @Test
    void shouldReturnAccountListPayload() {
        FinanceService service = mock(FinanceService.class);
        FinanceController controller = new FinanceController(service);
        when(service.listAccountsByPropertyId("P-1"))
            .thenReturn(List.of(new ChartOfAccountResponse(1L, "P-1", "REV", "Revenue", "REVENUE", "LIABILITY", true)));

        var response = controller.listAccounts("P-1").getBody();

        assertEquals(1, response.data().size());
    }
}

