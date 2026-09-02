package com.pms.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenVerifierTest {

    private static final String SECRET = "pms-auth-super-secret-key-change-me-in-prod-2026";

    @Test
    void verify_shouldReturnUsernameAndRolesForValidAccessToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String token = tokenService.generateAccessToken(userId, "john.doe", Set.of("ADMIN", "STAFF"));

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        Optional<AccessTokenVerifier.VerifiedAccessToken> verified = verifier.verify(token);

        assertThat(verified).isPresent();
        assertThat(verified.get().userId()).isEqualTo(userId);
        assertThat(verified.get().username()).isEqualTo("john.doe");
        assertThat(verified.get().roles()).containsExactlyInAnyOrder("ADMIN", "STAFF");
    }

    @Test
    void verify_shouldRejectRefreshToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String refreshToken = tokenService.generateRefreshToken(userId, "john.doe");

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        assertThat(verifier.verify(refreshToken)).isEmpty();
    }

    @Test
    void verifyRefreshToken_shouldReturnUserIdForValidRefreshToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String refreshToken = tokenService.generateRefreshToken(userId, "john.doe");

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        assertThat(verifier.verifyRefreshToken(refreshToken)).contains(userId);
    }

    @Test
    void verify_shouldRejectTamperedToken() {
        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        assertThat(verifier.verify("not-a-real-jwt")).isEmpty();
    }

    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new AccessTokenVerifier("too-short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes long");
    }
}
