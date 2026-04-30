package com.frontdesk.pms.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceClient implements PropertyLookupService {

    private final RestTemplate restTemplate;

    @Value("${services.property-service.url:http://localhost:8081}")
    private String propertyServiceUrl;

    @Override
    public boolean exists(UUID propertyId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(propertyServiceUrl)
                .path("/api/properties/{propertyId}")
                .buildAndExpand(propertyId)
                .toUri();

        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(uri, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        } catch (RestClientException ex) {
            log.error("Unable to validate propertyId={} using property-service at {}", propertyId, propertyServiceUrl, ex);
            throw ex;
        }
    }
}
