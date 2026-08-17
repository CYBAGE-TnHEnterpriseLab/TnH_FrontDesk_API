package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.dashboard.config.DashboardProperties;
import com.pms.dashboard.service.model.DashboardModels;
import com.pms.guestlisting.exception.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class HousekeepingDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public HousekeepingDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getHousekeeping().getBaseUrl()).build(), objectMapper);
        this.config = properties.getHousekeeping();
    }

    public Mono<DashboardModels.HousekeepingDashboardData> fetchDashboard(UUID propertyId, LocalDate businessDate) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getPrimaryPath())
                        .queryParam("propertyId", propertyId)
                        .queryParam("businessDate", businessDate)
                        .build(),
                "housekeeping.dashboard"
        ).map(this::toDashboardData);
    }

    public Mono<List<DashboardModels.HousekeepingRoomData>> fetchRooms(UUID propertyId, LocalDate businessDate) {
        return getJson(
                webClient.get(),
                uri -> uri.path(config.getSecondaryPath())
                        .queryParam("propertyId", propertyId)
                        .queryParam("businessDate", businessDate)
                        .queryParam("page", 0)
                        .queryParam("size", 200)
                        .queryParam("sortBy", "roomNumber")
                        .queryParam("sortDir", "asc")
                        .build(),
                "housekeeping.rooms"
        ).map(this::toRooms);
    }

    private DashboardModels.HousekeepingDashboardData toDashboardData(JsonNode node) {
        if (node == null || node.isNull()) {
            return DashboardModels.HousekeepingDashboardData.empty();
        }
        return new DashboardModels.HousekeepingDashboardData(
                node.path("totalRooms").asLong(0),
                node.path("vacantClean").asLong(0),
                node.path("vacantDirty").asLong(0),
                node.path("occupiedClean").asLong(0),
                node.path("occupiedDirty").asLong(0),
                node.path("outOfOrder").asLong(0),
                node.path("outOfService").asLong(0),
                node.path("inspected").asLong(0),
                node.path("pickup").asLong(0),
                node.path("arrivals").asLong(0),
                node.path("departures").asLong(0)
        );
    }

    private List<DashboardModels.HousekeepingRoomData> toRooms(JsonNode rootNode) {
        JsonNode roomsNode = rootNode == null ? null : rootNode.path("rooms");
        if (roomsNode == null || !roomsNode.isArray()) {
            throw new ExternalServiceException("housekeeping.rooms response does not contain rooms array");
        }
        List<DashboardModels.HousekeepingRoomData> result = new ArrayList<>();
        for (JsonNode item : roomsNode) {
            UUID roomTypeId = null;
            String rawRoomTypeId = item.path("roomTypeId").asText(null);
            if (rawRoomTypeId != null && !rawRoomTypeId.isBlank()) {
                try {
                    roomTypeId = UUID.fromString(rawRoomTypeId);
                } catch (IllegalArgumentException ignored) {
                    roomTypeId = null;
                }
            }
            result.add(new DashboardModels.HousekeepingRoomData(
                    item.path("roomTypeName").asText(""),
                    roomTypeId,
                    item.path("cleaningStatus").asText(""),
                    item.path("frontOfficeStatus").asText(""),
                    item.path("sellable").asBoolean(false)
            ));
        }
        return result;
    }
}

