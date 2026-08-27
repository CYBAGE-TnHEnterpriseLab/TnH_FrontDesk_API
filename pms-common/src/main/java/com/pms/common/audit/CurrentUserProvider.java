package com.pms.common.audit;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {

    Optional<UUID> getCurrentUserId();
}