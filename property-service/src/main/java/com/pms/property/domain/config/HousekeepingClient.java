package com.pms.property.domain.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HousekeepingClient {

    private final WebClient housekeepingWebClient;
    private final HttpServletRequest request;

    public HousekeepingClient(
            @Qualifier("housekeepingSyncWebClient") WebClient housekeepingWebClient,
            HttpServletRequest request
    ) {
        this.housekeepingWebClient = housekeepingWebClient;
        this.request = request;
    }

    public void deletePropertyData(String propertyId) {
        housekeepingWebClient.delete()
                .uri("/api/v1/housekeeping/room-master/properties/{propertyId}", propertyId)
                .header(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
