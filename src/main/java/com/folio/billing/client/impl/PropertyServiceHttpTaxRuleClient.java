package com.folio.billing.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.client.PropertyTaxRuleClient;
import com.folio.billing.config.IntegrationProperties;
import com.folio.billing.dto.PropertyTaxRule;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.ArrayList;
import java.util.List;

@Component
public class PropertyServiceHttpTaxRuleClient implements PropertyTaxRuleClient {
    private final IntegrationProperties.PropertyServiceConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PropertyServiceHttpTaxRuleClient(RestClient.Builder builder, IntegrationProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getPropertyService();
        this.restClient = builder.baseUrl(config.getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PropertyTaxRule> getTaxRules(String propertyId) {
        if (!StringUtils.hasText(propertyId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Property-Id is required to calculate taxes");
        try {
            String body = restClient.get().uri(config.getTaxRulesPath(), propertyId)
                    .headers(headers -> authorization(headers))
                    .retrieve().body(String.class);
            JsonNode rules = objectMapper.readTree(body).path("data");
            List<PropertyTaxRule> result = new ArrayList<>();
            if (rules.isArray()) for (JsonNode rule : rules) result.add(objectMapper.treeToValue(rule, PropertyTaxRule.class));
            return result;
        } catch (ResponseStatusException ex) { throw ex;
        } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to retrieve property tax rules", ex); }
    }

    private void authorization(HttpHeaders headers) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization)) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
