package com.pms.guestlisting.integration;

import com.pms.guestlisting.config.ReservationServiceProperties;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.exception.ExternalServiceException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
                    new HttpEntity<>(buildHeaders()),
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
                    new HttpEntity<>(buildHeaders()),
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch departures from Reservation Service", ex);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String incomingAuthorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(incomingAuthorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, incomingAuthorization);
            }
        }
        return headers;
    }
}

