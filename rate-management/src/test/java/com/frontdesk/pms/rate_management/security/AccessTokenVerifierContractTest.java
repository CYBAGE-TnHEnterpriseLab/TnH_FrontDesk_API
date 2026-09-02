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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenVerifierContractTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "abcdef0123456789abcdef0123456789";

    private final AccessTokenVerifier accessTokenVerifier = new AccessTokenVerifier(JWT_SECRET);

    @Test
    void verify_shouldAcceptValidAccessTokenWithRoles() {
        String token = buildToken("admin-user", "access", List.of("ADMIN"));

        Optional<AccessTokenVerifier.VerifiedAccessToken> verifiedToken = accessTokenVerifier.verify(token);

        assertTrue(verifiedToken.isPresent());
        assertEquals("admin-user", verifiedToken.get().username());
        assertEquals(Set.of("ADMIN"), verifiedToken.get().roles());
    }

    @Test
    void verify_shouldRejectRefreshToken() {
        String token = buildToken("admin-user", "refresh", List.of("ADMIN"));

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    @Test
    void verify_shouldRejectTokenWithMissingTypeClaim() {
        String token = buildTokenWithoutType("admin-user", List.of("ADMIN"));

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    @Test
    void verify_shouldAcceptCommaSeparatedRoles() {
        String token = buildTokenWithRolesClaim("admin-user", "access", "ADMIN, MANAGER");

        Optional<AccessTokenVerifier.VerifiedAccessToken> verifiedToken = accessTokenVerifier.verify(token);

        assertTrue(verifiedToken.isPresent());
        assertEquals(Set.of("ADMIN", "MANAGER"), verifiedToken.get().roles());
    }

    @Test
    void verify_shouldAllowEmptyRolesClaim() {
        String token = buildToken("admin-user", "access", List.of());

        assertTrue(accessTokenVerifier.verify(token).isPresent());
        assertTrue(accessTokenVerifier.verify(token).get().roles().isEmpty());
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
        String token = buildTokenWithSecret("admin-user", "access", List.of("ADMIN"), OTHER_SECRET);

        assertTrue(accessTokenVerifier.verify(token).isEmpty());
    }

    private String buildToken(String username, String tokenType, List<String> roles) {
        return buildTokenWithSecret(username, tokenType, roles, JWT_SECRET);
    }

    private String buildTokenWithSecret(String username, String tokenType, Object rolesClaim, String secret) {
        return Jwts.builder()
                .subject(username)
                .claim("typ", tokenType)
                .claim("roles", rolesClaim)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String buildTokenWithoutType(String username, List<String> roles) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String buildTokenWithRolesClaim(String username, String tokenType, String rolesClaim) {
        return buildTokenWithSecret(username, tokenType, rolesClaim, JWT_SECRET);
    }
}
