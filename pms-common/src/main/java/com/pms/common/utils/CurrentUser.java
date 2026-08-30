package com.pms.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID userId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No authenticated user"
            );
        }

        return UUID.fromString(
                authentication.getName()
        );
    }

    public static Authentication authentication() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication();
    }

    public static boolean hasRole(
            String role
    ) {

        Authentication authentication =
                authentication();

        if (authentication == null) {
            return false;
        }

        String authority =
                role.startsWith("ROLE_")
                        ? role
                        : "ROLE_" + role;

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority()
                                .equals(authority)
                );
    }
}