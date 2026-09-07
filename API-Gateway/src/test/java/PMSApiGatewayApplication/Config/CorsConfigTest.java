package PMSApiGatewayApplication.Config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

class CorsConfigTest {

    @Test
    void preflightRequestAllowsConfiguredOrigin() {
        WebTestClient client = buildClient("http://localhost:4200");

        client.options()
                .uri("/api/test")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    @Test
    void preflightRequestRejectsDisallowedOrigin() {
        WebTestClient client = buildClient("http://localhost:4200");

        client.options()
                .uri("/api/test")
                .header(HttpHeaders.ORIGIN, "http://malicious.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void configuredOriginPatternsAreTrimmedAndEmptyValuesIgnored() {
        WebTestClient client = buildClient("http://localhost:4200, , https://example.com  ");

        client.options()
                .uri("/api/test")
                .header(HttpHeaders.ORIGIN, "https://example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com");
    }

    private WebTestClient buildClient(String allowedPatterns) {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOriginPatterns", allowedPatterns);

        WebHandler handler = exchange -> {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        };

        return WebTestClient.bindToWebHandler(handler)
                .webFilter(corsConfig.corsWebFilter())
                .configureClient()
                .baseUrl("http://localhost")
                .build();
    }
}


