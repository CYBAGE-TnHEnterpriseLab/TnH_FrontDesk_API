package com.folio.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.client.PropertyTaxRuleClient;
import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.dto.FolioChargePostRequest;
import com.folio.billing.dto.FolioChargePostResponse;
import com.folio.billing.dto.PropertyTaxRule;
import com.folio.billing.repository.FolioTaxSnapshotRepository;
import com.folio.billing.service.impl.BillingFolioServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingFolioServiceImplTest {
    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void calculatesAndPersistsMultipleAddOnTaxes() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        PropertyTaxRuleClient taxRules = mock(PropertyTaxRuleClient.class);
        FolioTaxSnapshotRepository snapshots = mock(FolioTaxSnapshotRepository.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
        when(taxRules.getTaxRules("property-1")).thenReturn(List.of(
                rule("GST", "18"), rule("City Tax", "2")
        ));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Property-Id", "property-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, taxRules, snapshots, new ObjectMapper());
        FolioChargePostResponse response = service.addCharge(new FolioChargePostRequest(
                "CONF-1", "101", "Guest", "MINIBAR", "Water", new BigDecimal("100.00"), LocalDate.of(2026, 8, 13), "agent"));

        assertEquals(new BigDecimal("20.00"), response.tax());
        assertEquals(new BigDecimal("120.00"), response.totalAmount());
        assertEquals(2, response.taxDetails().size());
        verify(snapshots).save(any());
    }

    @Test
    void doesNotFetchAddOnRulesForRoomCharges() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        PropertyTaxRuleClient taxRules = mock(PropertyTaxRuleClient.class);
        FolioTaxSnapshotRepository snapshots = mock(FolioTaxSnapshotRepository.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, taxRules, snapshots, new ObjectMapper());

        FolioChargePostResponse response = service.addCharge(new FolioChargePostRequest(
                "CONF-1", "101", "Guest", "ROOM", "Room", new BigDecimal("100.00"), null, "agent"));

        assertEquals(BigDecimal.ZERO.setScale(2), response.tax());
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
        verifyNoInteractions(taxRules);
    }

    private PropertyTaxRule rule(String name, String rate) {
        return new PropertyTaxRule(1L, "property-1", name, "PERCENTAGE", new BigDecimal(rate), "ADD_ON", "EXCLUSIVE", LocalDate.of(2026, 1, 1), true, "ACTIVE", 1);
    }
}
