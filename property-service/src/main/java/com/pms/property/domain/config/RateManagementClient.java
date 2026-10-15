package com.pms.property.domain.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RateManagementClient {

    private final WebClient rateManagementWebClient;
    private final HttpServletRequest request;

    public RateManagementClient(
            @Qualifier("rateManagementWebClient") WebClient rateManagementWebClient,
            HttpServletRequest request
    ) {
        this.rateManagementWebClient = rateManagementWebClient;
        this.request = request;
    }

    public void deletePropertyRatePlans(String propertyId) {
        rateManagementWebClient.delete()
                .uri("/api/rate-plans/property/{propertyId}", propertyId)
                .headers(headers -> {
                    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (auth != null && !auth.isBlank()) {
                        headers.set(HttpHeaders.AUTHORIZATION, auth);
                    }
                })
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void deleteAllMasterRoomsForProperty(String propertyId) {
        rateManagementWebClient.delete()
                .uri("/api/master-rooms/delete-all-master-rooms/property/{propertyId}", propertyId)
                .headers(headers -> {
                    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (auth != null && !auth.isBlank()) {
                        headers.set(HttpHeaders.AUTHORIZATION, auth);
                    }
                })
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}