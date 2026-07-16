package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RoomDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PropertyWizardClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.property-wizard.base-url}")
    private String propertyWizardBaseUrl;

    @Value("${services.property-wizard.endpoints.property-by-id}")
    private String propertyByIdPath;

    @Value("${services.property-wizard.endpoints.room-types-by-property}")
    private String roomTypesByPropertyPath;

    public boolean propertyExists(String propertyId) {
        try {
            JsonNode response = webClientBuilder.build()
                    .get()
                    .uri(propertyWizardBaseUrl + propertyByIdPath, Map.of("propertyId", propertyId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return false;
            }

            boolean success = !response.has("success") || response.path("success").asBoolean(true);
            JsonNode data = response.path("data");
            return success && !data.isMissingNode() && !data.isNull();
        } catch (WebClientResponseException.NotFound ex) {
            return false;
        } catch (WebClientResponseException ex) {
            throw mapPropertyWizardException("property validation", ex);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to communicate with Property Wizard during property validation",
                    ex);
        }
    }

    public RoomDTO[] getRoomTypesByProperty(String propertyId) {
        try {
            JsonNode response = webClientBuilder.build()
                    .get()
                    .uri(propertyWizardBaseUrl + roomTypesByPropertyPath, Map.of("propertyId", propertyId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                return new RoomDTO[0];
            }

            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return new RoomDTO[0];
            }

            return StreamSupport.stream(data.spliterator(), false)
                    .map(this::toRoomDTO)
                    .toArray(RoomDTO[]::new);
        } catch (WebClientResponseException.NotFound ex) {
            return new RoomDTO[0];
        } catch (WebClientResponseException ex) {
            throw mapPropertyWizardException("room type lookup", ex);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to communicate with Property Wizard during room type lookup",
                    ex);
        }
    }

    private ResponseStatusException mapPropertyWizardException(String operation, WebClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401) {
            return new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Property Wizard rejected authorization for " + operation,
                    ex);
        }
        if (status == 403) {
            return new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Property Wizard denied access for " + operation,
                    ex);
        }
        return new ResponseStatusException(ex.getStatusCode(), "Property Wizard request failed during " + operation, ex);
    }

    private RoomDTO toRoomDTO(JsonNode roomOutletTypeNode) {
        RoomDTO roomDTO = new RoomDTO();

        if (roomOutletTypeNode.hasNonNull("id")) {
            roomDTO.setId(roomOutletTypeNode.get("id").asLong());
        }

        String roomName = roomOutletTypeNode.path("roomName").asText(null);
        String roomCode = roomOutletTypeNode.path("roomCode").asText(null);

        // Fallbacks keep compatibility if environments still return inventory-room payloads.
        if (!StringUtils.hasText(roomName)) {
            roomName = roomOutletTypeNode.path("roomTypeName").asText(null);
        }
        if (!StringUtils.hasText(roomCode)) {
            roomCode = roomOutletTypeNode.path("roomNumber").asText(null);
        }

        if (StringUtils.hasText(roomName)) {
            roomDTO.setName(roomName);
        } else if (StringUtils.hasText(roomCode)) {
            roomDTO.setName(roomCode);
        } else {
            roomDTO.setName("UNKNOWN");
        }

        if (StringUtils.hasText(roomCode)) {
            roomDTO.setType(roomCode);
        } else if (StringUtils.hasText(roomName)) {
            roomDTO.setType(roomName);
        } else {
            roomDTO.setType("UNKNOWN");
        }

        roomDTO.setActive(roomOutletTypeNode.path("availableForSell").asBoolean(true));

        return roomDTO;
    }
}
