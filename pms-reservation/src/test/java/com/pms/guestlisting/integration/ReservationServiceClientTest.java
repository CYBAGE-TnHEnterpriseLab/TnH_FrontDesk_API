package com.pms.guestlisting.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.guestlisting.config.ReservationServiceProperties;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.exception.ExternalServiceException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ReservationServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ReservationServiceProperties properties;
    private ReservationServiceClient client;

    @BeforeEach
    void setUp() {
        properties = new ReservationServiceProperties();
        properties.setBaseUrl("http://reservation-service");
        properties.setArrivalsPath("/api/v1/arrivals");
        properties.setDeparturesPath("/api/v1/departures");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(3000);
        client = new ReservationServiceClient(restTemplate, properties);
    }

    @Test
    void fetchArrivalsShouldReturnResponseBody() {
        ReservationArrivalDto dto = new ReservationArrivalDto();
        dto.setConfirmationNumber("CNF-1001");

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
            any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of(dto)));

        List<ReservationArrivalDto> result = client.fetchArrivals("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", LocalDate.of(2026, 6, 3));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConfirmationNumber()).isEqualTo("CNF-1001");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
            any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
        assertThat(urlCaptor.getValue()).contains("propertyId=7cfd4559-b6f3-4b7d-b933-e93018ac1d47");
        assertThat(urlCaptor.getValue()).contains("businessDate=2026-06-03");
    }

    @Test
    void fetchArrivalsShouldReturnEmptyListWhenBodyIsNull() {
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
            any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        List<ReservationArrivalDto> result = client.fetchArrivals("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", LocalDate.of(2026, 6, 3));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchArrivalsShouldWrapRestClientException() {
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
            any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("upstream down"));

        assertThatThrownBy(() -> client.fetchArrivals("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", LocalDate.of(2026, 6, 3)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to fetch arrivals from Reservation Service");
    }

        @Test
        void fetchDeparturesShouldReturnResponseBody() {
        ReservationArrivalDto dto = new ReservationArrivalDto();
        dto.setConfirmationNumber("CNF-2001");

        when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of(dto)));

        List<ReservationArrivalDto> result = client.fetchDepartures("PROP002", LocalDate.of(2026, 6, 4));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConfirmationNumber()).isEqualTo("CNF-2001");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
            urlCaptor.capture(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        );
        assertThat(urlCaptor.getValue()).contains("/api/v1/departures");
        assertThat(urlCaptor.getValue()).contains("propertyId=PROP002");
        assertThat(urlCaptor.getValue()).contains("businessDate=2026-06-04");
        }

        @Test
        void fetchDeparturesShouldWrapRestClientException() {
        when(restTemplate.exchange(
            any(String.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("upstream down"));

        assertThatThrownBy(() -> client.fetchDepartures("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", LocalDate.of(2026, 6, 3)))
            .isInstanceOf(ExternalServiceException.class)
            .hasMessageContaining("Failed to fetch departures from Reservation Service");
        }

    @Test
    void fetchDeparturesShouldReturnEmptyListWhenBodyIsNull() {
        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
            any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        List<ReservationArrivalDto> result = client.fetchDepartures("7cfd4559-b6f3-4b7d-b933-e93018ac1d47", LocalDate.of(2026, 6, 3));

        assertThat(result).isEmpty();
    }
}

