package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.service.model.DashboardModels;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class InventoryDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public InventoryDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getInventory().getBaseUrl()).build(), objectMapper);
        this.config = properties.getInventory();
    }

    public Mono<DashboardModels.InventoryDailyData> fetchDaily(UUID propertyId, UUID roomTypeId, LocalDate businessDate) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getPrimaryPath())
                        .queryParam("propertyId", propertyId)
                        .queryParam("roomTypeId", roomTypeId)
                        .queryParam("businessDate", businessDate)
                        .build(),
                "inventory.daily"
        ).map(this::toDailyData);
    }

    private DashboardModels.InventoryDailyData toDailyData(JsonNode node) {
        if (node == null || node.isNull()) {
            return DashboardModels.InventoryDailyData.empty();
        }
        return new DashboardModels.InventoryDailyData(
                node.path("totalInventory").asInt(0),
                node.path("reservedCount").asInt(0),
                node.path("blockedCount").asInt(0),
                node.path("availableCount").asInt(0)
        );
    }
}

