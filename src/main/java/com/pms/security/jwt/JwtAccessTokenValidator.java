package com.pms.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAccessTokenValidator {

    private final AccessTokenVerifier accessTokenVerifier;

    public VerifiedAccessToken validate(String token) {
        return accessTokenVerifier.verify(token);
    }
}
