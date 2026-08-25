package com.folio.billing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class AccessTokenVerifier {

    private final JwtSecurityProperties securityProperties;

    public AccessTokenVerifier(JwtSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public VerifiedAccessToken verify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JwtValidationException("Token is missing");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            validateTokenType(claims);

            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                throw new JwtValidationException("Token subject is missing");
            }

            Instant expiresAt = claims.getExpiration() == null
                    ? null
                    : claims.getExpiration().toInstant();

            return new VerifiedAccessToken(
                    username,
                    extractRoles(claims.get("roles")),
                    expiresAt,
                    claims.getId()
            );
        } catch (JwtValidationException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException("Invalid access token", ex);
        }
    }

    private void validateTokenType(Claims claims) {
        String tokenType = claims.get("typ", String.class);
        if (!"access".equalsIgnoreCase(tokenType)) {
            throw new JwtValidationException("Token type is not access");
        }
    }

    private SecretKey signingKey() {
        String configuredSecret = securityProperties.getSecret();
        if (!StringUtils.hasText(configuredSecret)) {
            throw new JwtValidationException("JWT shared secret is not configured");
        }

        byte[] keyBytes = securityProperties.isSecretBase64Encoded()
                ? Decoders.BASE64.decode(configuredSecret)
                : configuredSecret.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private List<String> extractRoles(Object rolesClaim) {
        if (rolesClaim == null) {
            return List.of();
        }

        Stream<String> roleStream;

        if (rolesClaim instanceof String rolesString) {
            roleStream = Stream.of(rolesString.split(","));
        } else if (rolesClaim instanceof Collection<?> collection) {
            roleStream = collection.stream().map(String::valueOf);
        } else {
            roleStream = Stream.of(String.valueOf(rolesClaim));
        }

        return roleStream
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(role -> !"null".equalsIgnoreCase(role))
                .map(String::toUpperCase)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
