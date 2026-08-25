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

    public void updateCheckedInStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                      LocalDate departureDate, String roomNumber,
                                      String guestDisplayName, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                guestDisplayName, confirmationId, "OCCUPIED", "IN_HOUSE");
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
}
