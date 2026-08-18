package com.folio.billing.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityProperties {

    private String secret;
    private boolean secretBase64Encoded;
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/error"
    ));

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isSecretBase64Encoded() {
        return secretBase64Encoded;
    }

    public void setSecretBase64Encoded(boolean secretBase64Encoded) {
        this.secretBase64Encoded = secretBase64Encoded;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths == null ? new ArrayList<>() : publicPaths;
    }
}
