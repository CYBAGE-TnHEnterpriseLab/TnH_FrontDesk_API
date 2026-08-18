package com.pms.housekeeping.security;

import com.pms.housekeeping.common.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecuritySupportTest {

    private final RequestCurrentUserProvider currentUserProvider = new RequestCurrentUserProvider();

    @AfterEach
    void tearDown() {
        RequestUserContext.clear();
    }

    @Test
    void requestUserContext_shouldStoreAndClearUsername() {
        assertThat(RequestUserContext.getUsername()).isNull();

        RequestUserContext.setUsername("alice");
        assertThat(RequestUserContext.getUsername()).isEqualTo("alice");

        RequestUserContext.clear();
        assertThat(RequestUserContext.getUsername()).isNull();
    }

    @Test
    void requestCurrentUserProvider_shouldReturnUsernameFromThreadLocal() {
        RequestUserContext.setUsername("alice");

        assertThat(currentUserProvider.getCurrentUsername()).isEqualTo("alice");
    }

    @Test
    void requestCurrentUserProvider_shouldRejectMissingUsername() {
        RequestUserContext.clear();

        assertThatThrownBy(currentUserProvider::getCurrentUsername)
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authenticated user context missing");
    }

    @Test
    void jwtAccessTokenValidator_shouldRejectShortSecretAndInvalidToken() {
        assertThatThrownBy(() -> new JwtAccessTokenValidator("too-short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes long");

        JwtAccessTokenValidator validator = new JwtAccessTokenValidator("0123456789abcdef0123456789abcdef");
        assertThat(validator.validateAccessToken("not-a-real-jwt")).isEmpty();
    }
}


