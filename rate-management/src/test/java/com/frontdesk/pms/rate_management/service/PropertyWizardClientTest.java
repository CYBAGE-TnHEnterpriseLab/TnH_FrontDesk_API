package com.frontdesk.pms.rate_management.service;

import com.frontdesk.pms.rate_management.dto.RoomDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyWizardClientTest {

    private HttpServer server;
    private String baseUrl;
    private final Map<String, StubResponse> stubs = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handleRequest);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        stubs.clear();
    }

    @Test
    void propertyExists_shouldReturnTrueWhenSuccessAndDataPresent() {
        stubGet("/api/properties/P100", 200, "{\"success\":true,\"data\":{\"id\":\"P100\"}}");

        PropertyWizardClient client = createClient();

        assertTrue(client.propertyExists("P100"));
    }

    @Test
    void propertyExists_shouldReturnFalseWhenApiReturnsNotFound() {
        PropertyWizardClient client = createClient();

        assertFalse(client.propertyExists("UNKNOWN"));
    }

    @Test
    void propertyExists_shouldReturnFalseWhenSuccessFlagIsFalse() {
        stubGet("/api/properties/P200", 200, "{\"success\":false,\"data\":{\"id\":\"P200\"}}");

        PropertyWizardClient client = createClient();

        assertFalse(client.propertyExists("P200"));
    }

    @Test
    void propertyExists_shouldMapUpstreamUnauthorizedStatus() {
        stubGet("/api/properties/P401", 401, "{\"error\":\"unauthorized\"}");

        PropertyWizardClient client = createClient();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> client.propertyExists("P401"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
    }

    @Test
    void getRoomTypesByProperty_shouldMapUpstreamForbiddenStatus() {
        stubGet("/api/rooms/properties/P403/room-outlet-types", 403, "{\"error\":\"forbidden\"}");

        PropertyWizardClient client = createClient();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> client.getRoomTypesByProperty("P403"));
        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
    }

    @Test
    void getRoomTypesByProperty_shouldMapRoomOutletTypeFields() {
        stubGet(
                "/api/rooms/properties/P100/room-outlet-types",
                200,
                "{\"success\":true,\"data\":[{\"id\":101,\"roomName\":\"Deluxe\",\"roomCode\":\"DLX\",\"availableForSell\":false}]}"
        );

        PropertyWizardClient client = createClient();
        RoomDTO[] rooms = client.getRoomTypesByProperty("P100");

        assertEquals(1, rooms.length);
        assertEquals(101L, rooms[0].getId());
        assertEquals("Deluxe", rooms[0].getName());
        assertEquals("DLX", rooms[0].getType());
        assertFalse(rooms[0].isActive());
    }

    @Test
    void getRoomTypesByProperty_shouldFallbackToLegacyFieldsWhenRoomOutletFieldsMissing() {
        stubGet(
                "/api/rooms/properties/P101/room-outlet-types",
                200,
                "{\"success\":true,\"data\":[{\"id\":202,\"roomTypeName\":\"Legacy Deluxe\",\"roomNumber\":\"L-01\"}]}"
        );

        PropertyWizardClient client = createClient();
        RoomDTO[] rooms = client.getRoomTypesByProperty("P101");

        assertEquals(1, rooms.length);
        assertEquals(202L, rooms[0].getId());
        assertEquals("Legacy Deluxe", rooms[0].getName());
        assertEquals("L-01", rooms[0].getType());
        assertTrue(rooms[0].isActive());
    }

    private PropertyWizardClient createClient() {
        PropertyWizardClient client = new PropertyWizardClient(WebClient.builder());
        ReflectionTestUtils.setField(client, "propertyWizardBaseUrl", baseUrl);
        ReflectionTestUtils.setField(client, "propertyByIdPath", "/api/properties/{propertyId}");
        ReflectionTestUtils.setField(client, "roomTypesByPropertyPath", "/api/rooms/properties/{propertyId}/room-outlet-types");
        return client;
    }

    private void stubGet(String path, int status, String body) {
        stubs.put("GET " + path, new StubResponse(status, body));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        StubResponse response = stubs.get(key);

        if (response == null) {
            writeResponse(exchange, 404, "");
            return;
        }

        writeResponse(exchange, response.status, response.body);
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private record StubResponse(int status, String body) {
    }
}