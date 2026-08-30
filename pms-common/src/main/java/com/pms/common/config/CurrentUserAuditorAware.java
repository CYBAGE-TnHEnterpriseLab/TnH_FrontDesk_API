package com.pms.common.config;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

public class CurrentUserAuditorAware
        implements AuditorAware<UUID> {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserAuditorAware(
            CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return currentUserProvider.getCurrentUserId();
    }
}