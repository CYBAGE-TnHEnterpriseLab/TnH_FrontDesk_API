package com.pms.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "pms-auth-super-secret-key-change-me-in-prod-2026";

    @Test
    void accessTokenShouldContainSubjectRolesAndAccessType() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        String token = service.generateAccessToken("admin.user", Set.of("ADMIN"));

        var claims = service.parseAccessTokenClaims(token);
        assertThat(claims.getSubject()).isEqualTo("admin.user");
        assertThat(service.extractRoles(claims)).contains("ADMIN");
        assertThat(claims.get(JwtTokenService.TOKEN_TYPE_CLAIM)).isEqualTo("access");
    }

    @Test
    void refreshTokenShouldContainRefreshType() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        String token = service.generateRefreshToken("admin.user");

        var claims = service.parseRefreshTokenClaims(token);
        assertThat(claims.getSubject()).isEqualTo("admin.user");
        assertThat(claims.get(JwtTokenService.TOKEN_TYPE_CLAIM)).isEqualTo("refresh");
    }

    @Test
    void parseAccessTokenClaimsShouldRejectRefreshToken() {
        JwtTokenService service = new JwtTokenService(SECRET, 900, 1209600);
        String refreshToken = service.generateRefreshToken("admin.user");

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
