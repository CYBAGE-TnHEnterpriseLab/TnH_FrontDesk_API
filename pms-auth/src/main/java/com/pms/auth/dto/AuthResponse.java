package com.pms.auth.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Set;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long accessTokenExpiresInSeconds;
    private final Set<String> roles;
}

