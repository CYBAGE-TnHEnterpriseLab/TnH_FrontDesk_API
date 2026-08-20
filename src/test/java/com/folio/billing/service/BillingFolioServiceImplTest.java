package com.folio.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.dto.BillingDetailsResponse;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.FolioChargePostRequest;
import com.folio.billing.dto.FolioChargePostResponse;
import com.folio.billing.dto.FolioDetailsResponse;
import com.folio.billing.entity.Folio;
import com.folio.billing.repository.FolioRepository;
import com.folio.billing.service.impl.BillingFolioServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingFolioServiceImplTest {
    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void deserializesFlatAddChargeRequestPayload() throws Exception {
                ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FolioChargePostRequest request = objectMapper.readValue("""
                {
                                    "confirmationNo": "CONF-1",
                  "guestName": "Guest",
                  "category": "MINIBAR",
                  "description": "Water",
                  "amount": 100.00,
                  "postingDate": "2026-08-13",
                  "userId": "agent"
                }
                """, FolioChargePostRequest.class);

        assertEquals("CONF-1", request.confirmationNumber());
        assertEquals("CHARGE", request.transactionType());
        assertEquals("MINIBAR", request.category());
        assertEquals("Water", request.description());
        assertEquals(new BigDecimal("100.00"), request.amount());
        assertEquals(LocalDate.of(2026, 8, 13), request.postingDate());
        assertEquals("agent", request.userId());
    }

    @Test
    void addsChargeWithoutTax() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Property-Id", "property-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper());
        FolioChargePostResponse response = service.addCharge(new FolioChargePostRequest(
                                "CONF-1", "101", "Guest", "Accomodation", "Dinner", new BigDecimal("100.00"), LocalDate.of(2026, 8, 13), "agent"));

        assertEquals(new BigDecimal("100.00"), response.totalAmount());
    }

    @Test
        void addsConfiguredChargeCategoryWithoutTax() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper());

        FolioChargePostResponse response = service.addCharge(new FolioChargePostRequest(
                                "CONF-1", "101", "Guest", "Housekeeping", "Laundry", new BigDecimal("100.00"), null, "agent"));

        assertEquals(new BigDecimal("100.00"), response.totalAmount());
    }

        @Test
        void addChargeReturnsResponseWhenTransactionPersistenceFails() {
                ReservationServiceClient reservations = mock(ReservationServiceClient.class);
                FolioRepository folioRepository = mock(FolioRepository.class);
                when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());

                Instant now = Instant.now();
                Folio folioA = new Folio("CONF-1", "A", "Guest", "101", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, now, now);
                when(folioRepository.findAll()).thenReturn(List.of(folioA));
                when(folioRepository.findByConfirmationNumberOrderByFolioCode("CONF-1")).thenReturn(List.of(folioA));
                when(folioRepository.save(any(Folio.class))).thenThrow(new RuntimeException("db write failed"));

                BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper(), folioRepository);

                FolioChargePostResponse response = assertDoesNotThrow(() -> service.addCharge(new FolioChargePostRequest(
                                "CONF-1", "101", "Guest", "Accomodation", "Dinner", new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), "agent"
                )));

                assertNotNull(response);
                assertEquals("CONF-1", response.confirmationNumber());
                assertEquals(new BigDecimal("100.00"), response.totalAmount());
        }

        @Test
        void acceptsConfiguredChargeCategories() {
                ReservationServiceClient reservations = mock(ReservationServiceClient.class);
                when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
                BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper());

                FolioChargePostResponse response = assertDoesNotThrow(() -> service.addCharge(new FolioChargePostRequest(
                                "CONF-1", "101", "Guest", "Accomodation", "Dinner", new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), "agent"
                )));

                assertEquals("CHARGE", response.transactionType());
                assertEquals("Accomodation", response.category());
        }

        @Test
        void rejectsUnsupportedChargeCategory() {
                ReservationServiceClient reservations = mock(ReservationServiceClient.class);
                when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
                BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper());

                assertThrows(ResponseStatusException.class, () -> service.addCharge(new FolioChargePostRequest(
                                "CONF-1", "101", "Guest", "Minibar", "Water", new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), "agent"
                )));
        }

    @Test
    void getBillingDetailsDoesNotFailWhenDuplicateFoliosExist() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        FolioRepository folioRepository = mock(FolioRepository.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());

        Instant now = Instant.now();
        Folio folioA1 = new Folio("CONF-1", "A", "Guest", "101", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, now, now);
        Folio folioA2 = new Folio("CONF-1", "A", "Guest", "101", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, now, now);
        when(folioRepository.findAll()).thenReturn(List.of(folioA1, folioA2));
        when(folioRepository.findByConfirmationNumberOrderByFolioCode("CONF-1")).thenReturn(List.of(folioA1, folioA2));
        when(folioRepository.save(any(Folio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper(), folioRepository);

        BillingDetailsResponse response = service.getBillingDetails("CONF-1", null, null);

        assertNotNull(response);
        assertEquals("CONF-1", response.confirmationNumber());
        verify(folioRepository).save(any(Folio.class));
    }

    @Test
    void getFolioBillingReturnsRowsWhenAutoRefreshPersistenceFails() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        FolioRepository folioRepository = mock(FolioRepository.class);

        FolioBillingRow row = new FolioBillingRow(
                "",
                "Doe",
                "John",
                "101",
                "John Doe",
                "CHECKED_IN",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 22),
                2,
                "",
                "DELUXE",
                "1283737609"
        );

        when(reservations.searchFolioBilling(any(FolioBillingFilter.class))).thenReturn(List.of(row));
        when(folioRepository.findAll()).thenReturn(List.of());
        when(folioRepository.findByConfirmationNumberOrderByFolioCode("1283737609")).thenReturn(List.of());
        when(folioRepository.save(any(Folio.class))).thenThrow(new RuntimeException("db write failed"));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper(), folioRepository);

        List<FolioBillingRow> result = assertDoesNotThrow(() -> service.getFolioBilling(new FolioBillingFilter(
                null, null, null, "1283737609", null, null
        )));

        assertEquals(1, result.size());
        assertEquals("1283737609", result.get(0).confirmationNumber());
    }

    @Test
    void getBillingDetailsReturnsResponseWhenPersistenceFails() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        FolioRepository folioRepository = mock(FolioRepository.class);
        when(reservations.getReservationSummary(any(), any(), any())).thenReturn(Optional.empty());
        when(folioRepository.findAll()).thenReturn(List.of());
        when(folioRepository.findByConfirmationNumberOrderByFolioCode("1283737609")).thenReturn(List.of());
        when(folioRepository.save(any(Folio.class))).thenThrow(new RuntimeException("db write failed"));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper(), folioRepository);

        BillingDetailsResponse response = assertDoesNotThrow(() ->
                service.getBillingDetails("1283737609", null, null));

        assertNotNull(response);
        assertEquals("1283737609", response.confirmationNumber());
    }

    @Test
    void getFolioDetailsReturnsResponseWhenRepositoryReadFails() {
        ReservationServiceClient reservations = mock(ReservationServiceClient.class);
        FolioRepository folioRepository = mock(FolioRepository.class);
        when(folioRepository.findAll()).thenReturn(List.of());
        when(folioRepository.findByConfirmationNumberOrderByFolioCode("1283737609"))
                .thenThrow(new RuntimeException("db read failed"));

        BillingFolioServiceImpl service = new BillingFolioServiceImpl(reservations, new ObjectMapper(), folioRepository);

        FolioDetailsResponse response = assertDoesNotThrow(() ->
                service.getFolioDetails("1283737609"));

        assertNotNull(response);
        assertEquals("1283737609", response.confirmationNumber());
        assertNotNull(response.folios());
    }
}
