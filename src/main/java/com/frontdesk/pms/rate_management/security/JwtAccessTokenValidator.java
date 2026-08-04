package com.frontdesk.pms.rate_management.security;

import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenValidator {

    private final AccessTokenVerifier accessTokenVerifier;

    public JwtAccessTokenValidator(AccessTokenVerifier accessTokenVerifier) {
        this.accessTokenVerifier = accessTokenVerifier;
    }

    public AccessTokenVerifier.VerifiedAccessToken validate(String token) throws JwtException {
        return accessTokenVerifier.verify(token)
                .orElseThrow(() -> new JwtException("Invalid or expired access token"));
    }
}
