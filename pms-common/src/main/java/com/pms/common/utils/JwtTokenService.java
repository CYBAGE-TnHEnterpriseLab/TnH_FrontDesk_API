package com.pms.common.utils;

import com.pms.common.config.JwtProperties;
import com.pms.common.enums.JwtTokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

public class JwtTokenService {

    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String ROLES_CLAIM = "roles";

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenService(JwtProperties properties) {
        this.signingKey = buildSigningKey(properties.getSecret());
        this.accessTokenExpirationSeconds = properties.getAccessTokenExpirationSeconds();
        this.refreshTokenExpirationSeconds = properties.getRefreshTokenExpirationSeconds();
    }

    public JwtTokenService(String secret, long accessTokenExpirationSeconds, long refreshTokenExpirationSeconds) {
        this.signingKey = buildSigningKey(secret);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String generateAccessToken(UUID userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSeconds);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpirationSeconds);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAccessTokenClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
        return claims;
    }

    public Claims parseRefreshTokenClaims(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
        return claims;
    }

    public Set<String> extractRoles(Claims claims) {
        Object rawRoles = claims.get(ROLES_CLAIM);
        if (rawRoles instanceof List<?> roleList) {
            return roleList.stream()
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
            throw new JwtException("Invalid token type");
        }
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
}
