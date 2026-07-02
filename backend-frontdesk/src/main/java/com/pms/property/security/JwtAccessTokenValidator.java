package com.pms.property.security;

import com.pms.security.jwt.AccessTokenVerifier;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenValidator {

    private final AccessTokenVerifier accessTokenVerifier;

    public JwtAccessTokenValidator(@Value("${security.jwt.secret}") String secret) {
        this.accessTokenVerifier = new AccessTokenVerifier(secret);
    }

    public Optional<AccessTokenVerifier.VerifiedAccessToken> validateAccessToken(String token) {
        return accessTokenVerifier.verify(token);
    }
}

