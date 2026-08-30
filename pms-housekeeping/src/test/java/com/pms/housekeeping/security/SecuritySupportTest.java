package com.pms.housekeeping.security;

import com.pms.common.security.CurrentUserProvider;
import com.pms.common.security.RequestCurrentUserProvider;
import com.pms.common.security.RequestUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecuritySupportTest {

    private final CurrentUserProvider currentUserProvider = new RequestCurrentUserProvider();

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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user context missing");
    }
}
