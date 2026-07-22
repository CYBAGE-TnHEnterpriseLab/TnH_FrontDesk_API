package com.pms.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class AccessTokenVerifierTest {

    private static final String SECRET = "test-jwt-secret-key-min-32-bytes-12345";

    @Test
    void shouldValidateAccessTokenAndExtractRoles() {
        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        String token = buildToken("admin", "access", List.of("ADMIN"));

        AccessTokenVerifier.VerifiedAccessToken verified = verifier.verify(token).orElseThrow();

        assertEquals("admin", verified.username());
        assertEquals(List.of("ADMIN"), List.copyOf(verified.roles()));
    }

    @Test
    void shouldRejectRefreshToken() {
        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        String token = buildToken("admin", "refresh", List.of("ADMIN"));

        assertTrue(verifier.verify(token).isEmpty());
    }

    @Test
    void shouldRejectShortSecret() {
        try {
            new AccessTokenVerifier("short-secret");
        } catch (IllegalArgumentException ex) {
            assertEquals("JWT secret must be at least 32 bytes long", ex.getMessage());
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private String buildToken(String subject, String tokenType, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(subject)
            .claim("typ", tokenType)
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300)))
            .signWith(key)
            .compact();
    }
}

