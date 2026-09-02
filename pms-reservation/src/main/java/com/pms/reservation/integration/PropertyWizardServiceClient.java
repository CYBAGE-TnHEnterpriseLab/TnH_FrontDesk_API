package com.pms.reservation.integration;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.integration.dto.PropertyRoomOutletTypeDto;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

    @Override
    public List<PropertyRoomOutletTypeDto> fetchRoomOutletTypes(String propertyId) {
        if (!StringUtils.hasText(properties.getRoomOutletTypesPath())) {
            return List.of();
        }

        String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
            .path(properties.getRoomOutletTypesPath())
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
                PropertyRoomOutletTypeDto.class,
                "room outlet types"
            );
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to fetch room outlet types from Property Wizard service", ex);
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

    private JsonNode unwrapDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isObject() && root.has("data")) {
            return root.get("data");
        }
        return root;
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
