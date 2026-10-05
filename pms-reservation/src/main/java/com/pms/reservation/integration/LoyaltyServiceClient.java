package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.LoyaltyServiceProperties;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    private final LoyaltyServiceProperties properties;

    private static final WebClient WEB_CLIENT = WebClient.builder()
            .filter(propagateRequestHeaders())
            .build();

    public void ensureMembership(UUID guestId, UUID loyaltyProgramId) {
        if (guestId == null || loyaltyProgramId == null) {
            return;
        }

        try {
            URI url = URI.create(UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/api/v1/loyalty/internal/memberships/ensure")
                    .toUriString());

            EnsureMembershipRequest request = new EnsureMembershipRequest(guestId, loyaltyProgramId);

            WEB_CLIENT.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ExternalServiceException(
                                            "Failed to ensure loyalty membership: " + body)))
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            throw new ExternalServiceException("Failed to ensure loyalty membership: " + body, ex);
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to ensure loyalty membership", ex);
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

    private record EnsureMembershipRequest(UUID guestId, UUID loyaltyProgramId) {
    }
}
