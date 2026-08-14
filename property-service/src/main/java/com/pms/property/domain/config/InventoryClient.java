package com.pms.property.domain.config;

import com.pms.property.common.response.ApiResponse;
import com.pms.property.domain.property.dto.PropertyDeletionCheckResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient inventoryWebClient;
    private final HttpServletRequest request;

    public boolean hasActiveReservations(
            String propertyId,
            LocalDate businessDate
    ) {

        ApiResponse<PropertyDeletionCheckResponse> response =
                inventoryWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v1/inventory/properties/{propertyId}/deletion-check")
                                .queryParam("businessDate", businessDate)
                                .build(propertyId))
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                request.getHeader(HttpHeaders.AUTHORIZATION)
                        )
                        .retrieve()
                        .bodyToMono(
                                new ParameterizedTypeReference<
                                        ApiResponse<PropertyDeletionCheckResponse>>() {}
                        )
                        .block();

        return response != null
                && response.data() != null
                && response.data().hasActiveReservations();
    }
}