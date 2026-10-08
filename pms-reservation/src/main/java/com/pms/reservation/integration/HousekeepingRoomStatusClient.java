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

    /**
     * Check-in hit: set arrival date status to ARRIVAL.
     */
    public void updateReservationStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                        LocalDate departureDate, String roomNumber,
                                        String guestDisplayName, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                guestDisplayName, confirmationId, "OCCUPIED", "ARRIVAL", null);
    }

    /**
     * Check-in hit: set current date to IN_HOUSE.
     */
    public void updateCheckedInStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                      LocalDate departureDate, String roomNumber,
                                      String guestDisplayName, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                guestDisplayName, confirmationId, "OCCUPIED", "IN_HOUSE", null);
    }

    /**
     * Check-in completion hit: update all stay nights to IN_HOUSE and departure date to DEPARTURE.
     */
    public void updateCheckedInStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                    String roomNumber, String guestDisplayName, String confirmationId) {
        LocalDate businessDate = arrivalDate;
        while (businessDate.isBefore(departureDate)) {
            updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", "IN_HOUSE", null);
            businessDate = businessDate.plusDays(1);
        }
        if (!businessDate.isAfter(departureDate)) {
            updateStatus(propertyId, departureDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", "DEPARTURE", null);
        }
    }

    /**
     * Reservation creation hit: update all nights with ARRIVAL/STAY_OVER/DEPARTURE.
     */
    public void updateReservationStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                      String roomNumber, String guestDisplayName, String confirmationId) {
        LocalDate businessDate = arrivalDate;
        boolean firstNight = true;
        while (businessDate.isBefore(departureDate)) {
            updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", firstNight ? "ARRIVAL" : "STAY_OVER", null);
            firstNight = false;
            businessDate = businessDate.plusDays(1);
        }
        if (!businessDate.isAfter(departureDate)) {
            updateStatus(propertyId, departureDate, arrivalDate, departureDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", "DEPARTURE", null);
        }
    }

    /**
     * Reservation cancellation hit: clear stay dates and mark DIRTY.
     */
    public void clearReservationStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                     String roomNumber) {
        clearStay(propertyId, arrivalDate, departureDate, roomNumber, "DIRTY");
    }

    /**
     * Check-in revert hit: clear stay dates without changing cleaning status.
     */
    public void clearCheckedInStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                                   String roomNumber) {
        clearStay(propertyId, arrivalDate, departureDate, roomNumber, null);
    }

    /**
     * Room reassignment hit: release reservation assignment via housekeeping API.
     */
    public void clearReservationAssignment(UUID propertyId, String confirmationId, String roomNumber,
                                            LocalDate arrivalDate, LocalDate departureDate) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/housekeeping/reservations/{confirmationId}/release")
                .queryParam("propertyId", propertyId)
                .queryParam("roomNumber", roomNumber)
                .queryParam("arrivalDate", arrivalDate)
                .queryParam("departureDate", departureDate)
                .buildAndExpand(confirmationId)
                .toUriString();
        HttpHeaders headers = copyAuthorizationHeader(new HttpHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Integer.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to release the room assignment in Housekeeping service", ex);
        }
    }

    /**
     * Departure hit: mark departure date as DEPARTURE.
     */
    public void updateDepartureStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                                      LocalDate departureDate, String roomNumber, String confirmationId) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                null, confirmationId, "OCCUPIED", "DEPARTURE", null);
    }

    /**
     * Checkout hit: mark room as VACANT/NOT_RESERVED/DIRTY and clear guest details.
     */
    public void markRoomDirty(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                              LocalDate departureDate, String roomNumber) {
        updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                null, null, "VACANT", "NOT_RESERVED", "DIRTY");
    }

    private void clearStay(UUID propertyId, LocalDate arrivalDate, LocalDate departureDate,
                           String roomNumber, String cleaningStatus) {
        LocalDate businessDate = arrivalDate;
        while (businessDate.isBefore(departureDate)) {
            updateStatus(propertyId, businessDate, arrivalDate, departureDate, roomNumber,
                    null, null, "VACANT", "NOT_RESERVED", cleaningStatus);
            businessDate = businessDate.plusDays(1);
        }
    }

    /**
     * Occupancy update hit: update date range with ARRIVAL/STAY_OVER.
     */
    public void updateOccupancyForDateRange(UUID propertyId, LocalDate startDate, LocalDate endDate,
                                             String roomNumber, String guestDisplayName, String confirmationId) {
        LocalDate businessDate = startDate;
        boolean firstNight = true;
        while (businessDate.isBefore(endDate)) {
            updateStatus(propertyId, businessDate, startDate, endDate, roomNumber,
                    guestDisplayName, confirmationId, "OCCUPIED", firstNight ? "ARRIVAL" : "STAY_OVER", null);
            firstNight = false;
            businessDate = businessDate.plusDays(1);
        }
    }

    private void updateStatus(UUID propertyId, LocalDate businessDate, LocalDate arrivalDate,
                              LocalDate departureDate, String roomNumber,
                              String guestDisplayName, String confirmationId,
                              String frontOfficeStatus, String reservationStatus,
                              String cleaningStatus) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/housekeeping/rooms/{roomNumber}/updateRoom")
                .buildAndExpand(roomNumber)
                .toUriString();

        HousekeepingRoomStatusUpdateRequest request = new HousekeepingRoomStatusUpdateRequest(
                propertyId, businessDate, frontOfficeStatus, guestDisplayName, arrivalDate, departureDate,
                reservationStatus, confirmationId, "RESERVATION", cleaningStatus);
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
            String reservationStatus, String confirmationId, String sourceModule,
            String cleaningStatus) {}
}
