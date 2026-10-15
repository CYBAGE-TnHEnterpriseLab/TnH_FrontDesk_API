package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class HousekeepingRoomCalendarClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${housekeeping-service.base-url:http://localhost:8086}")
    private String baseUrl;

    public List<PropertyRoomInventoryDto> fetchRooms(String propertyId, LocalDate from, LocalDate to) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/housekeeping/rooms/calendar")
                .queryParam("propertyId", propertyId)
                .queryParam("fromDate", from)
                .queryParam("toDate", to)
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            List<PropertyRoomInventoryDto> rooms = new ArrayList<>();
            for (JsonNode type : root.path("roomTypes")) {
                for (JsonNode room : type.path("rooms")) {
                    PropertyRoomInventoryDto item = new PropertyRoomInventoryDto();
                    item.setRoomType(type.path("roomTypeName").asText(null));
                    item.setRoomNumber(room.path("roomNumber").asText(null));
                    item.setFloor(room.path("floor").asText(null));
                    item.setAvailableRooms(1);
                    rooms.add(item);
                }
            }
            return rooms;
        } catch (RestClientException | java.io.IOException ex) {
            throw new ExternalServiceException("Failed to fetch room calendar from Housekeeping service", ex);
        }
    }

    public Integer findRoomFloor(String propertyId, String roomNumber, LocalDate from, LocalDate to) {
        if (roomNumber == null || roomNumber.isBlank()) {
            return null;
        }

        return fetchRooms(propertyId, from, to).stream()
                .filter(room -> roomNumber.trim().equalsIgnoreCase(room.getRoomNumber()))
                .map(PropertyRoomInventoryDto::getFloor)
                .filter(floor -> floor != null && !floor.isBlank())
                .findFirst()
                .map(String::trim)
                .map(this::parseFloor)
                .orElse(null);
    }

    private Integer parseFloor(String floor) {
        if (!StringUtils.hasText(floor)) {
            return null;
        }
        try {
            return Integer.valueOf(floor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            String authorization = servlet.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
