package com.frontdesk.pms.room.service;

import com.frontdesk.pms.room.exception.PropertyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyValidationService {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.property-service.base-url:http://localhost:8081}")
    private String propertyServiceBaseUrl;

    public void assertPropertyExists(UUID propertyId) {
        try {
            restClientBuilder.build()
                    .get()
                    .uri(propertyServiceBaseUrl + "/api/properties/{propertyId}", propertyId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new PropertyNotFoundException(propertyId);
            }
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new IllegalStateException("Property service is unavailable", ex);
        }
    }
}
