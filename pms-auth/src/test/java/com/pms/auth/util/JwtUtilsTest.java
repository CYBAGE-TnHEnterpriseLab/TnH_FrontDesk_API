package com.pms.auth.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtUtilsTest {

    private static final String SECRET = "pms-auth-super-secret-key-change-me-in-prod-2026";
    private final JwtUtils jwtUtils = new JwtUtils(SECRET, 900, 1209600, true);

    @Test
    void accessTokenShouldContainSubjectAndRoles() {
        String token = jwtUtils.generateAccessToken("admin.user", Set.of("ADMIN"));
        Claims claims = jwtUtils.parseAccessTokenClaims(token);

        assertEquals("admin.user", claims.getSubject());
        assertTrue(jwtUtils.extractRoles(claims).contains("ADMIN"));
    }

    @Test
    void refreshTokenShouldContainRefreshType() {
        String token = jwtUtils.generateRefreshToken("admin.user");
        Claims claims = jwtUtils.parseRefreshTokenClaims(token);

        assertEquals("admin.user", claims.getSubject());
    }
}

