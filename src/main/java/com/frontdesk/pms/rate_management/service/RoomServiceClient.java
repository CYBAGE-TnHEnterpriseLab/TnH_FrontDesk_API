package com.frontdesk.pms.rate_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.frontdesk.pms.rate_management.dto.RoomDTO;

@Service
@RequiredArgsConstructor
public class RoomServiceClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.room-service.base-url}")
    private String roomServiceBaseUrl;

    public RoomDTO[] getAllRooms() {
        return webClientBuilder.build()
                .get()
                .uri(roomServiceBaseUrl + "/api/rooms")
                .retrieve()
                .bodyToMono(RoomDTO[].class)
                .block(); // Use block() for sync, or return Mono for async
    }

    public RoomDTO[] getRoomTypesByProperty(String propertyId) {
        return webClientBuilder.build()
                .get()
                .uri(roomServiceBaseUrl + "/api/room-types/property/" + propertyId)
                .retrieve()
                .bodyToMono(RoomDTO[].class)
                .block();
    }
}
