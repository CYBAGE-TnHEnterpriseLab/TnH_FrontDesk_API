package com.frontdesk.pms.rate_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class WebClientConfig {

    private static final String MISSING_AUTH_MESSAGE =
            "Missing or invalid Authorization header for Property Wizard call";

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder().filter(propagateAuthorizationHeader());
    }

    private ExchangeFilterFunction propagateAuthorizationHeader() {
        return (request, next) -> {
            String outboundAuthorization = request.headers().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(outboundAuthorization)) {
                if (!outboundAuthorization.startsWith("Bearer ")) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, MISSING_AUTH_MESSAGE));
                }
                return next.exchange(request);
            }

            String authorizationHeader = resolveRequiredAuthorizationHeader();

            ClientRequest authenticatedRequest = ClientRequest.from(request)
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader))
                    .build();

            return next.exchange(authenticatedRequest);
        };
    }

    private String resolveRequiredAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MISSING_AUTH_MESSAGE);
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MISSING_AUTH_MESSAGE);
        }
        return authorization;
    }
}
