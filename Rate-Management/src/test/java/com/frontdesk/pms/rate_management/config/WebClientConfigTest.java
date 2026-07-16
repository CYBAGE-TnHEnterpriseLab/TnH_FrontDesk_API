package com.frontdesk.pms.rate_management.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebClientConfigTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", this::handleEcho);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        if (server != null) {
            server.stop(0);
        }
        requestCount.set(0);
    }

    @Test
    void propagateAuthorizationHeader_shouldForwardIncomingBearerHeader() {
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        incomingRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer forwarded-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));

        WebClient.Builder webClientBuilder = new WebClientConfig().webClientBuilder();

        String responseBody = webClientBuilder.build()
                .get()
                .uri(baseUrl + "/echo")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("Bearer forwarded-token", responseBody);
        assertEquals(1, requestCount.get());
    }

    @Test
    void propagateAuthorizationHeader_shouldRejectWhenIncomingHeaderMissing() {
        RequestContextHolder.resetRequestAttributes();

        WebClient.Builder webClientBuilder = new WebClientConfig().webClientBuilder();

        Throwable thrown = assertThrows(Throwable.class, () -> webClientBuilder.build()
                .get()
                .uri(baseUrl + "/echo")
                .retrieve()
                .bodyToMono(String.class)
                .block());

        ResponseStatusException exception = extractResponseStatusException(thrown);
        assertNotNull(exception);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Missing or invalid Authorization header"));
        assertEquals(0, requestCount.get());
    }

    @Test
    void propagateAuthorizationHeader_shouldRejectWhenIncomingHeaderIsNotBearer() {
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        incomingRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));

        WebClient.Builder webClientBuilder = new WebClientConfig().webClientBuilder();

        Throwable thrown = assertThrows(Throwable.class, () -> webClientBuilder.build()
                .get()
                .uri(baseUrl + "/echo")
                .retrieve()
                .bodyToMono(String.class)
                .block());

        ResponseStatusException exception = extractResponseStatusException(thrown);
        assertNotNull(exception);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Missing or invalid Authorization header"));
        assertEquals(0, requestCount.get());
    }

    private void handleEcho(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();

        String authorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            authorization = "";
        }

        byte[] responseBytes = authorization.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    private ResponseStatusException extractResponseStatusException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResponseStatusException responseStatusException) {
                return responseStatusException;
            }
            current = current.getCause();
        }
        return null;
    }
}
