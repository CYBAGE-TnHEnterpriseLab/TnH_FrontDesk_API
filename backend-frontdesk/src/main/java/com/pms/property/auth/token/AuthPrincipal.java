package com.pms.property.auth.token;

public record AuthPrincipal(
    String username,
    long expiresAtEpochSeconds
) {
}

