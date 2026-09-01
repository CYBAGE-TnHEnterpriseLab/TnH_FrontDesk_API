package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class HousekeepingRoomStatusClient {
    private final RestTemplate restTemplate;

    @Value("${housekeeping-service.base-url:http://localhost:8086}")
    private String baseUrl;

    public void updateReservationStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                        LocalDate departureDate, String roomNumber,
                                        String guestDisplayName, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                guestDisplayName, confirmationId, "VACANT", "ARRIVAL");
    }

    public Integer getRoomFloor(String propertyId, String roomNumber) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/housekeeping/rooms/{roomNumber}")
                .queryParam("propertyId", propertyId)
                .buildAndExpand(roomNumber)
                .toUriString();
        try {
            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(copyAuthorizationHeader(new HttpHeaders())), RoomFloorResponse.class);
            String floor = response.getBody() == null ? null : response.getBody().floor();
            if (floor == null || floor.isBlank()) {
                throw new ExternalServiceException("Room floor is not configured in Housekeeping service");
            }
            try {
                return Integer.valueOf(floor.trim());
            } catch (NumberFormatException ex) {
                throw new ExternalServiceException("Room floor must be numeric in Housekeeping service", ex);
            }
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch room floor from Housekeeping service", ex);
        }
    }

    public void updateCheckedInStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                      LocalDate departureDate, String roomNumber,
                                      String guestDisplayName, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                guestDisplayName, confirmationId, "OCCUPIED", "IN_HOUSE");
    }

    public void updateCheckedInStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                    String roomNumber, String guestDisplayName, String confirmationId) {
        LocalDate businessDate = arrivalDate;
        while (businessDate.isBefore(departureDate)) {
            updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", "IN_HOUSE");
            businessDate = businessDate.plusDays(1);
        }
    }

    public void updateReservationStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                      String roomNumber, String guestDisplayName, String confirmationId) {
        LocalDate businessDate = arrivalDate;
        boolean firstNight = true;
        while (businessDate.isBefore(departureDate)) {
            updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "VACANT", firstNight ? "ARRIVAL" : "STAY_OVER");
            firstNight = false;
            businessDate = businessDate.plusDays(1);
        }
    }

    private void updateStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                              LocalDate departureDate, String roomNumber,
                              String guestDisplayName, String confirmationId,
                              String frontOfficeStatus, String reservationStatus) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/housekeeping/rooms/{roomNumber}/status")
                .buildAndExpand(roomNumber)
                .toUriString();

        HousekeepingRoomStatusUpdateRequest request = new HousekeepingRoomStatusUpdateRequest(
                propertyId, businessDate, frontOfficeStatus, guestDisplayName, arrivalDate, departureDate,
                reservationStatus, confirmationId, "RESERVATION");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpHeaders requestHeaders = copyAuthorizationHeader(headers);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH,
                    new HttpEntity<>(request, requestHeaders), String.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to update room status in Housekeeping service", ex);
        }
    }

    private HttpHeaders copyAuthorizationHeader(HttpHeaders headers) {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            String authorization = servlet.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }

    private record HousekeepingRoomStatusUpdateRequest(
            UUID propertyId, LocalDate businessDate, String frontOfficeStatus,
            String guestDisplayName, LocalDate arrivalDate, LocalDate departureDate,
            String reservationStatus, String confirmationId, String sourceModule) {}

    private record RoomFloorResponse(String roomNumber, String floor) {}
}
