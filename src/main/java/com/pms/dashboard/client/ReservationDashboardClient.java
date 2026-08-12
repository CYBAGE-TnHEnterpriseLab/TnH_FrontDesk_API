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
public class ReservationDashboardClient extends DashboardWebClientSupport {

    private final DashboardProperties.ServiceProperties config;

    public ReservationDashboardClient(WebClient.Builder builder, DashboardProperties properties, ObjectMapper objectMapper) {
        super(builder.baseUrl(properties.getReservation().getBaseUrl()).build(), objectMapper);
        this.config = properties.getReservation();
    }

    public Mono<DashboardModels.ReservationFlowData> fetchFlow(UUID propertyId, LocalDate businessDate) {
        String listPath = config.getPrimaryPath();
        Mono<Long> arrivals = fetchCount(listPath, propertyId, businessDate, "arrivals", "reservation.arrivals");
        Mono<Long> departures = fetchCount(listPath, propertyId, businessDate, "departures", "reservation.departures");
        return Mono.zip(arrivals, departures)
                .map(tuple -> new DashboardModels.ReservationFlowData(tuple.getT1(), tuple.getT2()));
    }

    private Mono<Long> fetchCount(String path, UUID propertyId, LocalDate businessDate, String view, String sourceName) {
        return getJson(
                webClient.get(),
                uri -> uri.path(path)
                        .queryParam("propertyId", propertyId)
                        .queryParam("businessDate", businessDate)
                        .queryParam("view", view)
                        .queryParam("page", 0)
                        .queryParam("includeOptions", false)
                        .build(),
                sourceName
        ).map(this::countElements);
    }

    private long countElements(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.has("totalElements") && node.get("totalElements").canConvertToLong()) {
            return node.get("totalElements").asLong();
        }
        if (node.has("content") && node.get("content").isArray()) {
            return node.get("content").size();
        }
        if (node.isArray()) {
            return node.size();
        }
        return 0;
    }
}

