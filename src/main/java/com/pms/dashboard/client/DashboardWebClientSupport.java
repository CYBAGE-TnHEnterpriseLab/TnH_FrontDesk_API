package com.pms.dashboard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.guestlisting.exception.ExternalServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;

public abstract class DashboardWebClientSupport {

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected DashboardWebClientSupport(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    protected Mono<JsonNode> getJson(WebClient.RequestHeadersUriSpec<?> methodSpec,
                                     Function<UriBuilder, URI> uriFunction,
                                     String sourceName) {
        return methodSpec
                .uri(uriFunction)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new ExternalServiceException(sourceName + " call failed: " + body)))
                .bodyToMono(String.class)
                .map(this::readTreeSafe)
                .map(this::unwrapDataNode);
    }

    protected JsonNode unwrapDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isObject() && root.has("data")) {
            return root.get("data");
        }
        return root;
    }

    private JsonNode readTreeSafe(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (Exception ex) {
            throw new ExternalServiceException("Failed to parse downstream response", ex);
        }
    }
}

