package com.pms.housekeeping.security;

import com.pms.housekeeping.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class RequestCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String getCurrentUsername() {
        String username = RequestUserContext.getUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Authenticated user context missing");
        }
        return username;
    }
}

