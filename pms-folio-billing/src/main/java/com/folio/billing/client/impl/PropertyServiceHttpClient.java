package com.folio.billing.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.client.PropertyServiceClient;
import com.folio.billing.config.IntegrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Component
public class PropertyServiceHttpClient implements PropertyServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyServiceHttpClient.class);

    private final IntegrationProperties.ServiceConfig propertyServiceConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PropertyServiceHttpClient(
            RestClient.Builder restClientBuilder,
            IntegrationProperties integrationProperties,
            ObjectMapper objectMapper
    ) {
        this.propertyServiceConfig = integrationProperties.getPropertyService();
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(propertyServiceConfig.getBaseUrl()))
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> getChargeTypes(String propertyId) {
        if (!StringUtils.hasText(propertyId) || !propertyServiceConfig.isEnabled()
                || !StringUtils.hasText(propertyServiceConfig.getBaseUrl())) {
            return List.of();
        }

        try {
            String payload = restClient.get()
                    .uri("/api/finance/properties/{propertyId}/revenue-mappings", propertyId.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        ServletRequestAttributes attributes =
                                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                        if (attributes != null) {
                            String authorization = attributes.getRequest().getHeader("Authorization");
                            if (StringUtils.hasText(authorization)) {
                                headers.set("Authorization", authorization);
                            }
                        }
                    })
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return List.of();
            }
            return java.util.stream.StreamSupport.stream(data.spliterator(), false)
                    .map(node -> node.path("chargeType").asText(""))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            LOGGER.warn("Unable to load charge types for property {}: {}", propertyId, ex.getMessage());
            return List.of();
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8082";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
