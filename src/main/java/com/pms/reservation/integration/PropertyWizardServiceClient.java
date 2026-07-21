package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.integration.dto.InventoryDeductionRequest;
import com.pms.reservation.integration.dto.InventorySyncRequest;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyRoomInventoryDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class PropertyWizardServiceClient implements PropertyInventoryPort {

    private final RestTemplate restTemplate;
    private final PropertyWizardServiceProperties properties;
    private final ObjectMapper objectMapper;

    public PropertyInventoryValidationResponse validateInventory(String propertyId, String roomType, Integer requestedRooms) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventoryValidationPath())
                .queryParam("propertyId", propertyId)
                .queryParam("roomType", roomType)
                .queryParam("requestedRooms", requestedRooms)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );
            PropertyInventoryValidationResponse body = readObjectResponseBody(
                    response.getBody(),
                    PropertyInventoryValidationResponse.class,
                    "inventory validation"
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
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, buildHeaders()),
                    Void.class
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to deduct inventory in Property Wizard service", ex);
        }
    }

    public void syncInventory(InventorySyncRequest request) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getInventorySyncPath())
                .toUriString();

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, buildHeaders()),
                    Void.class
            );
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
                .queryParam("arrivalDate", arrivalDate)
                .queryParam("departureDate", departureDate);

        if (StringUtils.hasText(roomType)) {
            builder.queryParam("roomType", roomType);
        }

        String url = builder.buildAndExpand(propertyId).toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );
            List<PropertyRoomInventoryDto> parsedInventory = readListResponseBody(
                    response.getBody(),
                    PropertyRoomInventoryDto.class,
                    "inventory availability"
            );
            return normalizeInventoryRows(parsedInventory);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch live inventory from Property Wizard service", ex);
        }
    }

    public List<PropertyTaxRuleResponseDto> fetchTaxRules(String propertyId) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .path(properties.getTaxRulesPath())
                .buildAndExpand(propertyId)
                .toUriString();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );
            return readListResponseBody(
                    response.getBody(),
                    PropertyTaxRuleResponseDto.class,
                    "tax rules"
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch tax rules from Property Wizard service", ex);
        }
    }

    private <T> List<T> readListResponseBody(String body, Class<T> itemType, String operation) {
        if (!StringUtils.hasText(body)) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = unwrapDataNode(root);
            if (dataNode == null || dataNode.isNull()) {
                return Collections.emptyList();
            }

            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
            if (dataNode.isArray()) {
                return objectMapper.readerFor(listType).readValue(dataNode);
            }

            if (dataNode.isObject()) {
                T single = objectMapper.treeToValue(dataNode, itemType);
                return single == null ? Collections.emptyList() : List.of(single);
            }

            return Collections.emptyList();
        } catch (Exception ex) {
            throw new ExternalServiceException(
                    "Failed to parse " + operation + " response from Property Wizard service",
                    ex
            );
        }
    }

    private <T> T readObjectResponseBody(String body, Class<T> type, String operation) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = unwrapDataNode(root);
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, type);
        } catch (Exception ex) {
            throw new ExternalServiceException(
                    "Failed to parse " + operation + " response from Property Wizard service",
                    ex
            );
        }
    }

    private JsonNode unwrapDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isObject() && root.has("data")) {
            return root.get("data");
        }
        return root;
    }

    private List<PropertyRoomInventoryDto> normalizeInventoryRows(List<PropertyRoomInventoryDto> inventoryRows) {
        if (inventoryRows == null || inventoryRows.isEmpty()) {
            return Collections.emptyList();
        }

        boolean hasExplicitAvailabilityCount = inventoryRows.stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(item -> item.getAvailableRooms() != null);
        if (hasExplicitAvailabilityCount) {
            return inventoryRows;
        }

        Map<String, PropertyRoomInventoryDto> groupedByRoomType = new LinkedHashMap<>();
        for (PropertyRoomInventoryDto item : inventoryRows) {
            if (item == null) {
                continue;
            }

            String key;
            if (item.getRoomTypeId() != null) {
                key = "id:" + item.getRoomTypeId();
            } else if (StringUtils.hasText(item.getRoomType())) {
                key = "name:" + normalize(item.getRoomType());
            } else {
                continue;
            }

            PropertyRoomInventoryDto aggregate = groupedByRoomType.computeIfAbsent(key, ignored -> {
                PropertyRoomInventoryDto dto = new PropertyRoomInventoryDto();
                dto.setRoomTypeId(item.getRoomTypeId());
                dto.setRoomType(item.getRoomType());
                dto.setOccupancy(item.getOccupancy());
                dto.setAvailableRooms(0);
                return dto;
            });

            aggregate.setAvailableRooms((aggregate.getAvailableRooms() == null ? 0 : aggregate.getAvailableRooms()) + 1);
            if (!StringUtils.hasText(aggregate.getRoomType()) && StringUtils.hasText(item.getRoomType())) {
                aggregate.setRoomType(item.getRoomType());
            }
            if (aggregate.getRoomTypeId() == null && item.getRoomTypeId() != null) {
                aggregate.setRoomTypeId(item.getRoomTypeId());
            }
            if (!StringUtils.hasText(aggregate.getOccupancy()) && StringUtils.hasText(item.getOccupancy())) {
                aggregate.setOccupancy(item.getOccupancy());
            }
        }

        return new ArrayList<>(groupedByRoomType.values());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String incomingAuthorization = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(incomingAuthorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, incomingAuthorization);
            }
        }
        return headers;
    }
}
