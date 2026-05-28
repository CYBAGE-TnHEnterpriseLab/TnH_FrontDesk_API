package com.frontdesk.pms.rate_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class PropertyServiceClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.property-service.base-url}")
    private String propertyServiceBaseUrl;

    public boolean propertyExists(String propertyId) {
        try {
            webClientBuilder.build()
                    .get()
                    .uri(propertyServiceBaseUrl + "/api/properties/" + propertyId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (WebClientResponseException.NotFound ex) {
            return false;
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to validate propertyId " + propertyId + " with property service", ex);
        }
    }
}
