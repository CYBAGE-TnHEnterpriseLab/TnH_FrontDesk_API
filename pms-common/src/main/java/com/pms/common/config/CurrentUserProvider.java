package com.pms.common.config;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {

    Optional<UUID> getCurrentUserId();
}