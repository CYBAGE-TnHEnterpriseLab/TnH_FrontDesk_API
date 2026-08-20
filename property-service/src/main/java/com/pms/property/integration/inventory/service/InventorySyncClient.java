package com.pms.property.integration.inventory.service;

import com.pms.property.integration.inventory.dto.InventoryReconciliationRequest;
import com.pms.property.integration.inventory.dto.RoomMasterSyncRequest;
import com.pms.property.integration.inventory.exception.InventorySyncException;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

@Component
public class InventorySyncClient {

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

    public int reconcile(InventoryReconciliationRequest request, String requestId) {
        Map<String, Integer> response = postWithRetry(
            inventoryWebClient,
            "/api/v1/inventory/reconciliation",
            request,
            requestId,
            "Inventory reconciliation failed: ",
            "Inventory reconciliation request failed"
        );
        return response == null ? 0 : response.getOrDefault("affectedRows", 0);
    }

    public int syncRoomMaster(RoomMasterSyncRequest request, String requestId) {
        Map<String, Integer> response = postWithRetry(
            housekeepingWebClient,
            "/api/v1/housekeeping/room-master/sync",
            request,
            requestId,
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
        String errorPrefix,
        String requestFailureMessage
    ) {
        try {
            WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(uri)
                .header("X-Request-Id", requestId);

            authHeaderProvider.resolveAuthorizationHeader()
                .ifPresent(header -> requestSpec.header("Authorization", header));

            Map<String, Integer> response = requestSpec
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                    .map(bodyText -> new InventorySyncException(errorPrefix + bodyText)))
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Integer>>() {
                })
                .retryWhen(Retry.backoff(2, Duration.ofMillis(250)))
                .block(timeout);

            return response;
        } catch (InventorySyncException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InventorySyncException(requestFailureMessage, ex);
        }
    }
}


