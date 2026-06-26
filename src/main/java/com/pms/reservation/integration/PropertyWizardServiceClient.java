package com.pms.reservation.integration;

import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.integration.dto.InventoryDeductionRequest;
import com.pms.reservation.integration.dto.InventorySyncRequest;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class PropertyWizardServiceClient implements PropertyInventoryPort {

    private final RestTemplate restTemplate;
    private final PropertyWizardServiceProperties properties;

    public PropertyInventoryValidationResponse validateInventory(String propertyId, String roomType, Integer requestedRooms) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventoryValidationPath())
                .queryParam("propertyId", propertyId)
                .queryParam("roomType", roomType)
                .queryParam("requestedRooms", requestedRooms)
                .toUriString();

        try {
            PropertyInventoryValidationResponse body = restTemplate.getForObject(
                    url,
                    PropertyInventoryValidationResponse.class
            );
            if (body == null) {
                throw new ExternalServiceException("Property Wizard service returned empty validation response");
            }
            return body;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to validate property/room details with Property Wizard service", ex);
        }
    }

    public void deductInventory(InventoryDeductionRequest request) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventoryDeductionPath())
                .toUriString();

        try {
            restTemplate.postForLocation(url, request);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to deduct inventory in Property Wizard service", ex);
        }
    }

    public void syncInventory(InventorySyncRequest request) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventorySyncPath())
                .toUriString();

        try {
            restTemplate.postForLocation(url, request);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to sync inventory in Property Wizard service", ex);
        }
    }

    public List<PropertyRoomInventoryDto> fetchLiveInventory(
            String propertyId,
            LocalDate arrivalDate,
            LocalDate departureDate,
            String roomType
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventoryAvailabilityPath())
                .queryParam("propertyId", propertyId)
                .queryParam("arrivalDate", arrivalDate)
                .queryParam("departureDate", departureDate);

        if (StringUtils.hasText(roomType)) {
            builder.queryParam("roomType", roomType);
        }

        String url = builder.toUriString();

        try {
            ResponseEntity<List<PropertyRoomInventoryDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch live inventory from Property Wizard service", ex);
        }
    }
}
