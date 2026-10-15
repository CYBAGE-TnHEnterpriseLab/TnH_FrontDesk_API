package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.config.AuthServiceProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final AuthServiceProperties properties;

    public void verifyPassword(String password) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getVerifyPasswordPath())
                    .toUriString();
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("password", password), headers()),
                    Void.class);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BadRequestException("Password verification failed");
            }
            throw new ExternalServiceException("Password verification service unavailable", ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Password verification failed", ex);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return headers;
    }
}