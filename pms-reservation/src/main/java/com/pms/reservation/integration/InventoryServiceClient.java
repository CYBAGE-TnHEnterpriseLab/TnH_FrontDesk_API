package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.InventoryServiceProperties;
import com.pms.reservation.integration.dto.InventoryReservationRequest;
import com.pms.reservation.integration.dto.InventoryReservationResponse;
import com.pms.reservation.integration.dto.InventoryAvailabilityDto;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final InventoryServiceProperties properties;

    public InventoryReservationResponse reserve(InventoryReservationRequest request) {
        try {
            ResponseEntity<InventoryReservationResponse> response = restTemplate.exchange(
                    UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                            .path(properties.getReservationsPath()).toUriString(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers()),
                    InventoryReservationResponse.class
            );
            return response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to reserve inventory", ex);
        }
    }

    public void release(String confirmationNumber) {
        try {
            restTemplate.exchange(
                    UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                            .path(properties.getReservationsPath())
                            .pathSegment(confirmationNumber, "release")
                            .toUriString(),
                    HttpMethod.POST,
                    new HttpEntity<>(headers()),
                    InventoryReservationResponse.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to release inventory", ex);
        }
    }

    public List<InventoryAvailabilityDto> availability(
            String propertyId, String roomTypeId, LocalDate fromDate, LocalDate toDate) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getAvailabilityPath())
                    .queryParam("propertyId", propertyId)
                    .queryParam("roomTypeId", roomTypeId)
                    .queryParam("fromDate", fromDate)
                    .queryParam("toDate", toDate)
                    .toUriString();
            ResponseEntity<InventoryAvailabilityDto[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), InventoryAvailabilityDto[].class);
            return response.getBody() == null ? List.of() : Arrays.asList(response.getBody());
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch inventory availability", ex);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String authorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
