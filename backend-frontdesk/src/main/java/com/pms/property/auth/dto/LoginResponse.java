package com.pms.property.auth.dto;

public record LoginResponse(
    String username,
    String tokenType,
    String accessToken,
    long expiresAtEpochSeconds
) {
}

