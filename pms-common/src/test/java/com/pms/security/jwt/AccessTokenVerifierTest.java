package com.pms.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessTokenVerifierTest {

    private static final String SECRET = "pms-auth-super-secret-key-change-me-in-prod-2026";

    @Test
    void verify_shouldReturnUsernameAndRolesForValidAccessToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        String token = tokenService.generateAccessToken("john.doe", Set.of("ADMIN", "STAFF"));

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        Optional<AccessTokenVerifier.VerifiedAccessToken> verified = verifier.verify(token);

        assertThat(verified).isPresent();
        assertThat(verified.get().username()).isEqualTo("john.doe");
        assertThat(verified.get().roles()).containsExactlyInAnyOrder("ADMIN", "STAFF");
    }

    @Test
    void verify_shouldRejectRefreshToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        String refreshToken = tokenService.generateRefreshToken("john.doe");

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        assertThat(verifier.verify(refreshToken)).isEmpty();
    }

    @Test
    void verifyRefreshToken_shouldReturnUsernameForValidRefreshToken() {
        JwtTokenService tokenService = new JwtTokenService(SECRET, 900, 1209600);
        String refreshToken = tokenService.generateRefreshToken("john.doe");

        AccessTokenVerifier verifier = new AccessTokenVerifier(SECRET);
        assertThat(verifier.verifyRefreshToken(refreshToken)).contains("john.doe");
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
