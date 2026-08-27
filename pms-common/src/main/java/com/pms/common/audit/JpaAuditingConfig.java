package com.pms.common.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

@AutoConfiguration
@EnableJpaAuditing(
        auditorAwareRef = "currentUserAuditorAware"
)
public class JpaAuditingConfig {
    @Bean("currentUserAuditorAware")
    public AuditorAware<UUID> currentUserAuditorAware(
            CurrentUserProvider currentUserProvider) {

        return new CurrentUserAuditorAware(currentUserProvider);
    }
}