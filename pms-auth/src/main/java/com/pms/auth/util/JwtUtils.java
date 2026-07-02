package com.pms.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    @Autowired
    public JwtUtils(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            @Value("${security.jwt.refresh-token-expiration-seconds}") long refreshTokenExpirationSeconds
    ) {
        this.signingKey = buildSigningKey(secret);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    JwtUtils(String secret, long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds, boolean testMode) {
        this.signingKey = buildSigningKey(secret);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String generateAccessToken(String username, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, "access")
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpirationSeconds);

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAccessTokenClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, "access");
        return claims;
    }

    public Claims parseRefreshTokenClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, "refresh");
        return claims;
    }

    public Set<String> extractRoles(Claims claims) {
        Object rawRoles = claims.get(ROLES_CLAIM);
        if (rawRoles instanceof List<?> roles) {
            return roles.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    public Instant extractExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException("Invalid token type");
        }
    }

    private SecretKey buildSigningKey(String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes long");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}

