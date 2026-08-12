package com.pms.dashboard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardWebClientConfig {

    @Bean
    public WebClient.Builder dashboardWebClientBuilder() {
        return WebClient.builder().filter(propagateRequestHeaders());
    }

    private ExchangeFilterFunction propagateRequestHeaders() {
        return (request, next) -> {
            ClientRequest updatedRequest = request;
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String auth = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                String correlationId = servletAttrs.getRequest().getHeader("X-Correlation-Id");
                updatedRequest = ClientRequest.from(request)
                        .headers(headers -> {
                            if (StringUtils.hasText(auth)) {
                                headers.set(HttpHeaders.AUTHORIZATION, auth);
                            }
                            if (StringUtils.hasText(correlationId)) {
                                headers.set("X-Correlation-Id", correlationId);
                            }
                        })
                        .build();
            }
            return next.exchange(updatedRequest);
        };
    }
}

