package com.frontdesk.pms.rate_management.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenVerifierContractTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "abcdef0123456789abcdef0123456789";

    private AccessTokenVerifier accessTokenVerifier;

    @BeforeEach
    void setUp() {
        accessTokenVerifier = new AccessTokenVerifier();
        ReflectionTestUtils.setField(accessTokenVerifier, "jwtSecret", JWT_SECRET);
    }

    @Test
    void verifyAccessToken_shouldAcceptValidAccessTokenWithRoles() {
        String token = buildToken("admin-user", "access", List.of("ADMIN"));

        AccessTokenVerifier.VerifiedAccessToken verifiedToken = accessTokenVerifier.verifyAccessToken(token);

        assertEquals("admin-user", verifiedToken.username());
        assertEquals(List.of("ADMIN"), verifiedToken.roles());
    }

    @Test
    void verifyAccessToken_shouldRejectRefreshToken() {
        String token = buildToken("admin-user", "refresh", List.of("ADMIN"));

        assertThrows(JwtException.class, () -> accessTokenVerifier.verifyAccessToken(token));
    }

    @Test
    void verifyAccessToken_shouldRejectTokenWithoutRoles() {
        String token = buildToken("admin-user", "access", List.of());

        assertThrows(JwtException.class, () -> accessTokenVerifier.verifyAccessToken(token));
    }

    @Test
    void verifyAccessToken_shouldRejectShortJwtSecret() {
        AccessTokenVerifier verifierWithShortSecret = new AccessTokenVerifier();
        ReflectionTestUtils.setField(verifierWithShortSecret, "jwtSecret", "short-secret");

        assertThrows(JwtException.class, () -> verifierWithShortSecret.verifyAccessToken("invalid-token"));
    }

    @Test
    void verifyAccessToken_shouldRejectTokenSignedWithDifferentSecret() {
        String token = buildTokenWithSecret("admin-user", "access", List.of("ADMIN"), OTHER_SECRET);

        assertThrows(JwtException.class, () -> accessTokenVerifier.verifyAccessToken(token));
    }

    private String buildToken(String username, String tokenType, List<String> roles) {
        return buildTokenWithSecret(username, tokenType, roles, JWT_SECRET);
    }

    private String buildTokenWithSecret(String username, String tokenType, List<String> roles, String secret) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("typ", tokenType)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
