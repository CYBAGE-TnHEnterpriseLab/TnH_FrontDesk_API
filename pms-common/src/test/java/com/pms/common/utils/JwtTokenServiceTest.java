package com.pms.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "pms-auth-super-secret-key-change-me-in-prod-2026";

    @Test
    void accessTokenShouldContainSubjectRolesAndAccessType() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = service.generateAccessToken(userId, "admin.user", Set.of("ADMIN"));

        var claims = service.parseAccessTokenClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("username", String.class)).isEqualTo("admin.user");
        assertThat(service.extractRoles(claims)).contains("ADMIN");
        assertThat(claims.get(JwtTokenService.TOKEN_TYPE_CLAIM)).isEqualTo("access");
    }

    @Test
    void refreshTokenShouldContainRefreshType() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = service.generateRefreshToken(userId, "admin.user");

        var claims = service.parseRefreshTokenClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("username", String.class)).isEqualTo("admin.user");
        assertThat(claims.get(JwtTokenService.TOKEN_TYPE_CLAIM)).isEqualTo("refresh");
    }

    @Test
    void parseAccessTokenClaimsShouldRejectRefreshToken() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String refreshToken = service.generateRefreshToken(userId, "admin.user");

        assertThatThrownBy(() -> service.parseAccessTokenClaims(refreshToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new JwtTokenService("short", 900, 1209600))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes long");
    }
}
