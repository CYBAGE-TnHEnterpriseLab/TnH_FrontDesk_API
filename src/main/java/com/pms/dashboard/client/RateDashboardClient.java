package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.service.model.DashboardModels;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RateDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public RateDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getRate().getBaseUrl()).build(), objectMapper);
        this.config = properties.getRate();
    }

    public Mono<List<DashboardModels.RatePlanData>> fetchRatePlans(UUID propertyId) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getPrimaryPath()).build(propertyId.toString()),
                "rate.listPlans"
        ).map(this::toRatePlans);
    }

    private List<DashboardModels.RatePlanData> toRatePlans(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<DashboardModels.RatePlanData> result = new ArrayList<>();
        for (JsonNode item : node) {
            String name = item.path("name").asText(item.path("code").asText("UNKNOWN"));
            BigDecimal amount = parseDecimal(item.path("manualAmount").asText(null));
            if (amount == null) {
                amount = parseDecimal(item.path("baseRate").asText(null));
            }
            result.add(new DashboardModels.RatePlanData(name, amount));
        }
        return result;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

