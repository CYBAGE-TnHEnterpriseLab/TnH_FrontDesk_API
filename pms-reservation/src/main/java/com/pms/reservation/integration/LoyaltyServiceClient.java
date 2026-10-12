package com.pms.reservation.integration;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.LoyaltyServiceProperties;
import com.pms.reservation.integration.dto.LoyaltyApiResponse;
import com.pms.reservation.integration.dto.LoyaltyMembershipResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;

@Component
@RequiredArgsConstructor
public class LoyaltyServiceClient {

    private static final String MEMBERSHIPS_PATH = "/api/v1/loyalty/internal/memberships";

    private final LoyaltyServiceProperties properties;

    private static final WebClient WEB_CLIENT = WebClient.builder()
            .filter(propagateRequestHeaders())
            .build();

    public boolean isDefaultProgramConfigured() {
        return StringUtils.hasText(properties.getDefaultProgramId());
    }

    /**
     * Enrols the guest in the single loyalty program configured for the property, creating the membership
     * only when the guest does not already hold one. Existing memberships are returned unchanged.
     */
    public LoyaltyMembershipResponse ensureMembership(UUID guestId) {
        return ensureMembership(guestId, requireDefaultProgramId());
    }

    /**
     * Enrols the guest in the loyalty program the caller selected, creating the membership only when the
     * guest does not already hold one for that program. Existing memberships are returned unchanged.
     *
     * Memberships are scoped per guest and program, so the same guest can later be enrolled in a
     * different program of the same brand and receive a second membership.
     */
    public LoyaltyMembershipResponse ensureMembership(UUID guestId, UUID loyaltyProgramId) {
        if (loyaltyProgramId == null) {
            throw new LoyaltyProgramNotConfiguredException(
                    "A loyalty program is required to enrol the guest in a membership");
        }
        return postMembership(guestId, loyaltyProgramId, MEMBERSHIPS_PATH + "/ensure",
                new MembershipRequest(guestId, loyaltyProgramId), "ensure loyalty membership");
    }

    /**
     * Validates a loyalty number and returns the membership it is assigned to.
     */
    public LoyaltyMembershipResponse findMembershipByLoyaltyNumber(String loyaltyNumber) {
        URI url = URI.create(UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(MEMBERSHIPS_PATH)
                .path("/verify")
                .toUriString());

        try {
            return WEB_CLIENT.post()
                    .uri(url)
                    .bodyValue(new LoyaltyNumberRequest(loyaltyNumber.trim()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ExternalServiceException(
                                            "Failed to verify loyalty number: " + body)))
                    .bodyToMono(membershipResponseType())
                    .block()
                    .getData();
        } catch (WebClientResponseException ex) {
            throw new ExternalServiceException(
                    "Failed to verify loyalty number: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to verify loyalty number", ex);
        }
    }

    private UUID requireDefaultProgramId() {
        if (StringUtils.hasText(properties.getDefaultProgramId())) {
            return UUID.fromString(properties.getDefaultProgramId());
        }
        throw new LoyaltyProgramNotConfiguredException(
                "No default loyalty program is configured; cannot enrol the guest in a membership");
    }

    private LoyaltyMembershipResponse postMembership(
            UUID guestId,
            UUID loyaltyProgramId,
            String path,
            Object request,
            String operation
    ) {
        if (guestId == null) {
            throw new ExternalServiceException("A guest id is required to " + operation);
        }

        URI url = URI.create(UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(path)
                .toUriString());

        try {
            return WEB_CLIENT.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ExternalServiceException(
                                            "Failed to " + operation + ": " + body)))
                    .bodyToMono(membershipResponseType())
                    .block()
                    .getData();
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            throw new ExternalServiceException("Failed to " + operation + ": " + body, ex);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to " + operation, ex);
        }
    }

    /**
     * pms-loyalty wraps every response in ApiResponse, so the membership payload has to be read through
     * that envelope instead of being mapped directly, which would discard it and leave the number and tier null.
     */
    private static ParameterizedTypeReference<LoyaltyApiResponse<LoyaltyMembershipResponse>> membershipResponseType() {
        return new ParameterizedTypeReference<>() {
        };
    }

    /**
     * Raised when a guest is asked to be enrolled but no loyalty program is configured.
     * Mapped to a client error because the request cannot be fulfilled as submitted.
     */
    public static class LoyaltyProgramNotConfiguredException extends BadRequestException {
        public LoyaltyProgramNotConfiguredException(String message) {
            super(message);
        }
    }

    private static ExchangeFilterFunction propagateRequestHeaders() {
        return (request, next) -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String auth = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(auth)) {
                    ClientRequest updated = ClientRequest.from(request)
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, auth))
                            .build();
                    return next.exchange(updated);
                }
            }
            return next.exchange(request);
        };
    }

    private record MembershipRequest(UUID guestId, UUID loyaltyProgramId) {
    }

    private record LoyaltyNumberRequest(String loyaltyNumber) {
    }
}
