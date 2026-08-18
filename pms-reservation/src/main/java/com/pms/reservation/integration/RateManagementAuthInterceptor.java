package com.pms.reservation.integration;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RequiredArgsConstructor
public class RateManagementAuthInterceptor implements ClientHttpRequestInterceptor {

    private final String serviceAuthToken;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String token = resolveAuthorizationToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException(
                "Authorization token not available for Rate Management call. Configure rateManagement.serviceAuthToken or send incoming Authorization header"
            );
        }

        token = token.trim();
        if (!token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = "Bearer " + token;
        }

        request.getHeaders().set(HttpHeaders.AUTHORIZATION, token);
        return execution.execute(request, body);
    }

    private String resolveAuthorizationToken() {
        if (StringUtils.hasText(serviceAuthToken)) {
            return serviceAuthToken;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String incomingAuthorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(incomingAuthorization)) {
                return incomingAuthorization;
            }
        }

        return null;
    }
}
