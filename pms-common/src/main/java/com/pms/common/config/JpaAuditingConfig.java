package com.pms.common.config;

import com.pms.common.utils.CurrentUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@AutoConfiguration
public class JpaAuditingConfig {

    @Bean
    public com.pms.common.config.CurrentUserProvider jpaCurrentUserProvider() {
        return () -> {
            try {
                return Optional.of(CurrentUser.userId());
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        };
    }

    @Bean("currentUserAuditorAware")
    public AuditorAware<UUID> currentUserAuditorAware(
            com.pms.common.config.CurrentUserProvider jpaCurrentUserProvider) {

        return new CurrentUserAuditorAware(jpaCurrentUserProvider);
    }
}
