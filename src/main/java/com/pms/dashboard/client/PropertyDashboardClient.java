package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.service.model.DashboardModels;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PropertyDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public PropertyDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getProperty().getBaseUrl()).build(), objectMapper);
        this.config = properties.getProperty();
    }

    public Mono<List<DashboardModels.PropertyRoomTypeData>> fetchRoomTypes(UUID propertyId) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getPrimaryPath()).build(propertyId.toString()),
                "property.roomTypes"
        ).map(this::toRoomTypes);
    }

    private List<DashboardModels.PropertyRoomTypeData> toRoomTypes(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<DashboardModels.PropertyRoomTypeData> result = new ArrayList<>();
        for (JsonNode item : node) {
            UUID roomTypeId = null;
            String rawUuid = item.path("roomTypeId").asText(null);
            if (rawUuid != null && !rawUuid.isBlank()) {
                try {
                    roomTypeId = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException ignored) {
                    roomTypeId = null;
                }
            }
            String code = item.path("roomCode").asText("");
            String name = item.path("roomName").asText("");

            result.add(new DashboardModels.PropertyRoomTypeData(roomTypeId, code, name));
        }
        return result;
    }
}



