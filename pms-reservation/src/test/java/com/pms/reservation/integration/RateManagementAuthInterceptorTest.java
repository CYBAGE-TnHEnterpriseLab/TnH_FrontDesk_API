package com.pms.reservation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RateManagementAuthInterceptorTest {

    @Test
    void interceptShouldInjectBearerTokenWhenPrefixMissing() throws Exception {
        RateManagementAuthInterceptor interceptor = new RateManagementAuthInterceptor("service-token");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));
        AtomicReference<HttpRequest> capturedRequest = new AtomicReference<>();

        ClientHttpRequestExecution execution = (httpRequest, body) -> {
            capturedRequest.set(httpRequest);
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        };

        interceptor.intercept(request, new byte[0], execution);

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .isEqualTo("Bearer service-token");
    }

    @Test
    void interceptShouldKeepBearerPrefixWhenProvided() throws Exception {
        RateManagementAuthInterceptor interceptor = new RateManagementAuthInterceptor("Bearer abc-123");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));

        ClientHttpRequestExecution execution = (httpRequest, body) ->
            new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer abc-123");
    }

    @Test
    void interceptShouldFallbackToIncomingAuthorizationWhenServiceTokenMissing() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer incoming-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        try {
            RateManagementAuthInterceptor interceptor = new RateManagementAuthInterceptor(" ");
            MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));

            ClientHttpRequestExecution execution = (httpRequest, body) ->
                new MockClientHttpResponse(new byte[0], HttpStatus.OK);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer incoming-token");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void interceptShouldFailWhenServiceTokenAndIncomingAuthorizationMissing() {
        RateManagementAuthInterceptor interceptor = new RateManagementAuthInterceptor(" ");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/test"));

        ClientHttpRequestExecution execution = (httpRequest, body) ->
            new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Authorization token not available");
    }
}
