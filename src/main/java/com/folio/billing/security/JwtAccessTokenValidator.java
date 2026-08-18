package com.folio.billing.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAccessTokenValidator {

    private final AccessTokenVerifier accessTokenVerifier;

    public JwtAccessTokenValidator(AccessTokenVerifier accessTokenVerifier) {
        this.accessTokenVerifier = accessTokenVerifier;
    }

    public UsernamePasswordAuthenticationToken validate(String token) {
        VerifiedAccessToken verifiedAccessToken = accessTokenVerifier.verify(token);

        List<GrantedAuthority> authorities = verifiedAccessToken.roles().stream()
                .map(this::toRoleAuthority)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new UsernamePasswordAuthenticationToken(
                verifiedAccessToken.username(),
                null,
                authorities
        );
    }

    private String toRoleAuthority(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
