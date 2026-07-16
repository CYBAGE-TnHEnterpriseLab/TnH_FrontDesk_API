package com.frontdesk.pms.rate_management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class AccessTokenVerifier {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public VerifiedAccessToken verifyAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get("typ", String.class);
        if (!"access".equalsIgnoreCase(tokenType)) {
            throw new JwtException("Only access tokens are allowed");
        }

        String username = claims.getSubject();
        if (!StringUtils.hasText(username)) {
            throw new JwtException("Access token subject is missing");
        }

        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null) {
            throw new JwtException("Access token issued-at is missing");
        }

        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new JwtException("Access token expiration is missing");
        }

        Object rolesClaim = claims.get("roles");
        List<String> roles = extractRoles(rolesClaim);
        if (roles.isEmpty()) {
            throw new JwtException("Access token roles claim is missing or empty");
        }

        return new VerifiedAccessToken(username, roles);
    }

    private SecretKey getSigningKey() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new JwtException("JWT secret is missing");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new JwtException("JWT secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    private List<String> extractRoles(Object rolesClaim) {
        if (rolesClaim == null) {
            return List.of();
        }

        if (rolesClaim instanceof Collection<?> rolesCollection) {
            List<String> roles = new ArrayList<>();
            for (Object role : rolesCollection) {
                if (role instanceof String roleName && StringUtils.hasText(roleName)) {
                    roles.add(roleName.trim());
                }
            }
            return roles;
        }

        if (rolesClaim instanceof String rolesString && StringUtils.hasText(rolesString)) {
            String[] splitRoles = rolesString.split(",");
            List<String> roles = new ArrayList<>();
            for (String role : splitRoles) {
                if (StringUtils.hasText(role)) {
                    roles.add(role.trim());
                }
            }
            return roles;
        }

        return List.of();
    }

    public record VerifiedAccessToken(String username, List<String> roles) {
    }
}
