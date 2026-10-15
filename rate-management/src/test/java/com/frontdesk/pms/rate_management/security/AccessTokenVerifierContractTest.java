package com.frontdesk.pms.rate_management.security;

import com.pms.common.utils.AccessTokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenVerifierContractTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "abcdef0123456789abcdef0123456789";
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String USERNAME = "admin.user";

    private final AccessTokenVerifier accessTokenVerifier = new AccessTokenVerifier(JWT_SECRET);

    @Test
    void verify_shouldAcceptValidAccessTokenWithRoles() {
        String token = buildToken(USER_ID.toString(), USERNAME, "access", List.of("ADMIN"));

        Optional<AccessTokenVerifier.VerifiedAccessToken> verifiedToken = accessTokenVerifier.verify(token);

        assertTrue(verifiedToken.isPresent());
        assertEquals(USERNAME, verifiedToken.get().username());
        assertEquals(Set.of("ADMIN"), verifiedToken.get().roles());
    }

    @Test
    void verify_shouldRejectRefreshToken() {
        String token = buildToken(USER_ID.toString(), USERNAME, "refresh", List.of("ADMIN"));

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    @Test
    void verify_shouldRejectTokenWithMissingTypeClaim() {
        String token = buildTokenWithoutType(USER_ID.toString(), USERNAME, List.of("ADMIN"));

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    @Test
    void verify_shouldAcceptCommaSeparatedRoles() {
        String token = buildTokenWithRolesClaim(USER_ID.toString(), USERNAME, "access", "ADMIN, MANAGER");

        Optional<AccessTokenVerifier.VerifiedAccessToken> verifiedToken = accessTokenVerifier.verify(token);

        assertTrue(verifiedToken.isPresent());
        assertEquals(Set.of("ADMIN", "MANAGER"), verifiedToken.get().roles());
    }

    @Test
    void verify_shouldAllowEmptyRolesClaim() {
        String token = buildToken(USER_ID.toString(), USERNAME, "access", List.of());

        Optional<AccessTokenVerifier.VerifiedAccessToken> verifiedToken = accessTokenVerifier.verify(token);

        assertTrue(verifiedToken.isPresent());
        assertTrue(verifiedToken.get().roles().isEmpty());
    }

    @Test
    void verify_shouldRejectShortJwtSecret() {
        String shortSecret = "short-secret";

        try {
            new AccessTokenVerifier(shortSecret);
        } catch (IllegalArgumentException ex) {
            assertEquals("JWT secret must be at least 32 bytes long", ex.getMessage());
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for short JWT secret");
    }

    @Test
    void verify_shouldRejectTokenSignedWithDifferentSecret() {
        String token = buildTokenWithSecret(USER_ID.toString(), USERNAME, "access", List.of("ADMIN"), OTHER_SECRET);

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    private String buildToken(String subject, String username, String tokenType, List<String> roles) {
        return buildTokenWithSecret(subject, username, tokenType, roles, JWT_SECRET);
    }

    private String buildTokenWithSecret(String subject, String username, String tokenType, Object rolesClaim, String secret) {
        return Jwts.builder()
                .subject(subject)
                .claim("username", username)
                .claim("typ", tokenType)
                .claim("roles", rolesClaim)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String buildTokenWithoutType(String subject, String username, List<String> roles) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String buildTokenWithRolesClaim(String subject, String username, String tokenType, String rolesClaim) {
        return buildTokenWithSecret(subject, username, tokenType, rolesClaim, JWT_SECRET);
    }
}
