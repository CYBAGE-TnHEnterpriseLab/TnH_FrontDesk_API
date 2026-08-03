package com.frontdesk.pms.rate_management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AccessTokenVerifier {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ROLES_CLAIM = "roles";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final SecretKey signingKey;

    public AccessTokenVerifier(@Value("${security.jwt.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public Optional<VerifiedAccessToken> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
                return Optional.empty();
            }

            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new VerifiedAccessToken(username, extractRoles(claims)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Set<String> extractRoles(Claims claims) {
        Object rawRoles = claims.get(ROLES_CLAIM);
        if (rawRoles == null) {
            return Set.of();
        }

        Stream<String> roleStream;
        if (rawRoles instanceof List<?> roleList) {
            roleStream = roleList.stream().map(String::valueOf);
        } else if (rawRoles instanceof String roleString) {
            roleStream = Arrays.stream(roleString.split(","));
        } else {
            return Set.of();
        }

        return roleStream
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record VerifiedAccessToken(String username, Set<String> roles) {
    }
}
