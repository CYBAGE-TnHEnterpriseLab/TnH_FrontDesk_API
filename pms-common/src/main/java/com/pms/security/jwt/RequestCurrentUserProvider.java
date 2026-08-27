package com.pms.security.jwt;

import org.springframework.stereotype.Component;

@Component
public class RequestCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String getCurrentUsername() {
        String username = RequestUserContext.getUsername();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Authenticated user context missing");
        }
        return username;
    }
}
