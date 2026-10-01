package com.pms.property.integration.inventory.service;

import com.pms.property.integration.inventory.dto.InventoryReconciliationRequest;
import com.pms.property.integration.inventory.dto.RoomMasterSyncRequest;
import com.pms.property.integration.inventory.exception.InventorySyncException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class InventorySyncClient {

    private static final Logger log = LoggerFactory.getLogger(InventorySyncClient.class);

    private final WebClient inventoryWebClient;
    private final WebClient housekeepingWebClient;
    private final Duration timeout;
    private final InventorySyncAuthHeaderProvider authHeaderProvider;

    public InventorySyncClient(
        @Qualifier("inventorySyncWebClient") WebClient inventorySyncWebClient,
        @Qualifier("housekeepingSyncWebClient") WebClient housekeepingSyncWebClient,
        InventorySyncAuthHeaderProvider authHeaderProvider,
        @Value("${inventory.sync.timeout-seconds:8}") int timeoutSeconds
    ) {
        this.inventoryWebClient = inventorySyncWebClient;
        this.housekeepingWebClient = housekeepingSyncWebClient;
        this.authHeaderProvider = authHeaderProvider;
        this.timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 1));
    }

    public int reconcile(InventoryReconciliationRequest request, String requestId, String authHeader) {
        Map<String, Integer> response = postWithRetry(
            inventoryWebClient,
            "/api/v1/inventory/reconciliation",
            request,
            requestId,
            authHeader,
            "Inventory reconciliation failed: ",
            "Inventory reconciliation request failed"
        );
        return response == null ? 0 : response.getOrDefault("affectedRows", 0);
    }

    public int syncRoomMaster(RoomMasterSyncRequest request, String requestId, String authHeader) {
        Map<String, Integer> response = postWithRetry(
            housekeepingWebClient,
            "/api/v1/housekeeping/room-master/sync",
            request,
            requestId,
            authHeader,
            "Room master sync failed: ",
            "Room master sync request failed"
        );
        return response == null ? 0 : response.getOrDefault("syncedRooms", 0);
    }

    private Map<String, Integer> postWithRetry(
        WebClient webClient,
        String uri,
        Object body,
        String requestId,
        String authHeader,
        String errorPrefix,
        String requestFailureMessage
    ) {
        try {
            WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(uri)
                .header("X-Request-Id", requestId);

            String effectiveAuthHeader = authHeader != null && !authHeader.isBlank()
                    ? authHeader
                    : authHeaderProvider.resolveAuthorizationHeader().orElse(null);
            if (effectiveAuthHeader != null && !effectiveAuthHeader.isBlank()) {
                requestSpec.header("Authorization", effectiveAuthHeader);
            }

            Map<String, Integer> response = requestSpec
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    String status = String.valueOf(clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class)
                        .doOnNext(raw -> log.error("{}HTTP {} body={}", errorPrefix, status, raw))
                        .map(bodyText -> new InventorySyncException(errorPrefix + "HTTP " + status + ": " + bodyText));
                })
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Integer>>() {
                })
                .block(timeout);

            return response;
        } catch (InventorySyncException ex) {
            log.error("{}failed: {}", errorPrefix, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            log.error("{}{}", errorPrefix, msg, ex);
            throw new InventorySyncException(requestFailureMessage + ": " + msg, ex);
        }
    }
}


