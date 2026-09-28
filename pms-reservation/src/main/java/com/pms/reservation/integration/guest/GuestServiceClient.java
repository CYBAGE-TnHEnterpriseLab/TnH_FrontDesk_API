package com.pms.reservation.integration.guest;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.GuestServiceProperties;
import com.pms.reservation.integration.dto.GuestLookupRequest;
import com.pms.reservation.integration.dto.GuestProfileResponse;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GuestServiceClient {

    private static final String GUESTS_PATH = "/api/v1/guests";

    private final RestTemplate restTemplate;
    private final GuestServiceProperties properties;

    public GuestServiceClient(
            @Qualifier("guestServiceRestTemplate") RestTemplate restTemplate,
            GuestServiceProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<GuestProfileResponse> findExistingGuest(GuestLookupRequest request) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(GUESTS_PATH)
                .path("/lookup")
                .toUriString();

        try {
            ResponseEntity<GuestProfileResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, requestHeaders()),
                    GuestProfileResponse.class
            );
            return Optional.of(requireBody(response.getBody(), "guest lookup"));
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw downstreamFailure("guest lookup", ex);
        } catch (RestClientException ex) {
            throw downstreamFailure("guest lookup", ex);
        }
    }

    public Optional<GuestProfileResponse> getGuestById(Long guestProfileId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(GUESTS_PATH)
                .pathSegment(String.valueOf(guestProfileId))
                .toUriString();

        try {
            ResponseEntity<GuestProfileResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(requestHeaders()),
                    GuestProfileResponse.class
            );
            return Optional.of(requireBody(response.getBody(), "guest profile retrieval"));
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw downstreamFailure("guest profile retrieval", ex);
        } catch (RestClientException ex) {
            throw downstreamFailure("guest profile retrieval", ex);
        }
    }

    private GuestProfileResponse requireBody(GuestProfileResponse body, String operation) {
        if (body == null || body.getId() == null) {
            throw new ExternalServiceException(
                    "Guest service returned an empty or malformed response during " + operation);
        }
        return body;
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

    private ExternalServiceException downstreamFailure(String operation, Exception cause) {
        if (cause instanceof HttpStatusCodeException statusException) {
            return new ExternalServiceException(
                    "Guest service returned HTTP " + statusException.getStatusCode().value()
                            + " during " + operation,
                    cause
            );
        }
        return new ExternalServiceException(
                "Guest service request failed during " + operation,
                cause
        );
    }
}
