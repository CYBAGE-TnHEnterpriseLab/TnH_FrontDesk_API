package com.pms.housekeeping.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebConfigTest {

    @Test
    void openApiConfig_shouldBuildExpectedMetadata() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.housekeepingOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("PMS Housekeeping API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("REST APIs for PMS Housekeeping Microservice");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("PMS Team");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("support@pms.com");
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("Internal Use");
    }

    @Test
    void corsConfig_shouldRegisterConfiguredOriginsAndMethods() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOriginPatterns", "http://localhost:3000, https://app.example.com");

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOriginPatterns("http://localhost:3000", "https://app.example.com")).thenReturn(registration);
        when(registration.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")).thenReturn(registration);
        when(registration.allowedHeaders("*")).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);
        when(registration.maxAge(3600)).thenReturn(registration);

        config.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOriginPatterns("http://localhost:3000", "https://app.example.com");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        verify(registration).allowedHeaders("*");
        verify(registration).allowCredentials(true);
        verify(registration).maxAge(3600);
    }
}

