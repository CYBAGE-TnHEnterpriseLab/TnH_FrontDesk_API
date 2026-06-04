package com.hotel.pms.frontdesk.guestlisting.integration;

import com.hotel.pms.frontdesk.guestlisting.config.ReservationServiceProperties;
import com.hotel.pms.frontdesk.guestlisting.dto.ReservationArrivalDto;
import com.hotel.pms.frontdesk.guestlisting.exception.ExternalServiceException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class ReservationServiceClient {

    private final RestTemplate restTemplate;
    private final ReservationServiceProperties properties;

    public List<ReservationArrivalDto> fetchArrivals(String propertyId, LocalDate businessDate) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getArrivalsPath())
                .queryParam("propertyId", propertyId)
                .queryParam("businessDate", businessDate)
                .toUriString();

        try {
            ResponseEntity<List<ReservationArrivalDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch arrivals from Reservation Service", ex);
        }
    }

    public List<ReservationArrivalDto> fetchDepartures(String propertyId, LocalDate businessDate) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getDeparturesPath())
                .queryParam("propertyId", propertyId)
                .queryParam("businessDate", businessDate)
                .toUriString();

        try {
            ResponseEntity<List<ReservationArrivalDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch departures from Reservation Service", ex);
        }
    }
}
