package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.HousekeepingServiceProperties;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class HousekeepingRoomCalendarClient {
    private final RestTemplate restTemplate;
    private final HousekeepingServiceProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode fetchCalendar(String propertyId, LocalDate from, LocalDate to, String roomTypeId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(properties.getRoomCalendarPath()).queryParam("propertyId", propertyId)
            .queryParam("fromDate", from).queryParam("toDate", to);
        if (StringUtils.hasText(roomTypeId)) builder.queryParam("roomTypeId", roomTypeId);
        try {
            return objectMapper.readTree(restTemplate.exchange(builder.toUriString(), HttpMethod.GET,
                new HttpEntity<>(headers()), String.class).getBody());
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to fetch room calendar from Housekeeping service", ex);
        }
    }

    public void markAssigned(String roomNumber, JsonNode payload) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl()).path(properties.getRoomStatusPath())
            .buildAndExpand(roomNumber).toUriString();
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(payload, headers()), Void.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to update room status in Housekeeping service", ex);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization)) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return headers;
    }
}
