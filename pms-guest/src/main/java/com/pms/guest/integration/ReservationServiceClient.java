package com.pms.guest.integration;

import com.pms.guest.config.ReservationServiceProperties;
import com.pms.guest.exception.ReservationServiceException;
import com.pms.guest.integration.dto.ReservationApiResponse;
import com.pms.guest.integration.dto.ReservationGuestAssignment;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/** HTTP boundary to pms-reservation for reservation_guests / reservation_bookings data. */
@Component
public class ReservationServiceClient {

    private static final String ASSIGNMENTS_PATH = "/api/v1/reservation-guests/assignments";
    private static final ParameterizedTypeReference<ReservationApiResponse<List<ReservationGuestAssignment>>>
            ASSIGNMENTS_TYPE = new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final ReservationServiceProperties properties;

    public ReservationServiceClient(
            @Qualifier("reservationServiceRestTemplate") RestTemplate restTemplate,
            ReservationServiceProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public List<ReservationGuestAssignment> findAssignmentsByBookingId(String propertyId, Long bookingId) {
        return fetch(baseUri(propertyId).queryParam("bookingId", bookingId));
    }

    public List<ReservationGuestAssignment> findAssignmentsByConfirmationNumber(
            String propertyId,
            String confirmationNumber
    ) {
        return fetch(baseUri(propertyId).queryParam("confirmationNumber", confirmationNumber));
    }

    public List<ReservationGuestAssignment> findAssignmentsByGuestProfileIds(
            String propertyId,
            Collection<Long> guestProfileIds
    ) {
        if (guestProfileIds == null || guestProfileIds.isEmpty()) {
            return List.of();
        }
        return fetch(baseUri(propertyId).queryParam("guestProfileIds", guestProfileIds.toArray()));
    }

    private UriComponentsBuilder baseUri(String propertyId) {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(ASSIGNMENTS_PATH)
                .queryParam("propertyId", propertyId);
    }

    private List<ReservationGuestAssignment> fetch(UriComponentsBuilder uri) {
        try {
            ResponseEntity<ReservationApiResponse<List<ReservationGuestAssignment>>> response =
                    restTemplate.exchange(
                            uri.encode().build().toUri(),
                            HttpMethod.GET,
                            new HttpEntity<>(requestHeaders()),
                            ASSIGNMENTS_TYPE
                    );
            ReservationApiResponse<List<ReservationGuestAssignment>> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                throw new ReservationServiceException(
                        "Reservation service returned an empty or malformed guest assignment response");
            }
            return body.getData();
        } catch (HttpStatusCodeException ex) {
            throw new ReservationServiceException(
                    "Reservation service returned HTTP " + ex.getStatusCode().value()
                            + " during guest assignment lookup",
                    ex);
        } catch (RestClientException ex) {
            throw new ReservationServiceException(
                    "Reservation service request failed during guest assignment lookup", ex);
        }
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}
