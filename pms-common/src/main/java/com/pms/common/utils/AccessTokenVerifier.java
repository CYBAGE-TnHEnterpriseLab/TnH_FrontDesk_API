package com.pms.common.utils;

import com.pms.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

public class AccessTokenVerifier {

    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String ROLES_CLAIM = "roles";

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;

    public AccessTokenVerifier(JwtProperties properties) {
        this.signingKey = buildSigningKey(properties.getSecret());
    }

    public AccessTokenVerifier(String secret) {
        this.signingKey = buildSigningKey(secret);
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

            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isBlank()) {
                return Optional.empty();
            }
            UUID userId;
            try {
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }

            String username = claims.get("username", String.class);
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new VerifiedAccessToken(userId, username, extractRoles(claims)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<UUID> verifyRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                return Optional.empty();
            }

            String userIdStr = claims.getSubject();
            if (userIdStr == null || userIdStr.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
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

    private SecretKey buildSigningKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("JWT secret must be configured");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes long");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    public record VerifiedAccessToken(UUID userId, String username, Set<String> roles) {
    }
}
