package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.service.model.DashboardModels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PropertyDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public PropertyDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getProperty().getBaseUrl()).build(), objectMapper);
        this.config = properties.getProperty();
    }

    public Mono<List<DashboardModels.PropertyRoomTypeData>> fetchRoomTypes(UUID propertyId, String authorization) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getPrimaryPath()).build(propertyId.toString()),
                "property.roomTypes",
                authorization
        ).map(node -> toRoomTypes(node, propertyId));
    }

    private List<DashboardModels.PropertyRoomTypeData> toRoomTypes(JsonNode node, UUID propertyId) {
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
            if (roomTypeId == null) {
                String code = item.path("roomCode").asText("");
                String name = item.path("roomName").asText("");
                String roomKey = code.isBlank() ? name : code;
                if (roomKey.isBlank()) {
                    roomKey = "unknown";
                }
                String payload = (propertyId.toString() + ":" + roomKey).toLowerCase(Locale.ROOT);
                roomTypeId = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
            }
            String code = item.path("roomCode").asText("");
            String name = item.path("roomName").asText("");

            result.add(new DashboardModels.PropertyRoomTypeData(roomTypeId, code, name));
        }
        return result;
    }
}



