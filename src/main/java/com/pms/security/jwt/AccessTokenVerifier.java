package com.pms.security.jwt;

import com.pms.security.config.JwtSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AccessTokenVerifier {

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_ROLES = "roles";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtSecurityProperties jwtSecurityProperties;

    public VerifiedAccessToken verify(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecurityProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!ACCESS_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            throw new JwtException("Invalid token type");
        }

        String username = claims.getSubject();
        if (!StringUtils.hasText(username)) {
            throw new JwtException("Token subject is missing");
        }

        List<String> roles = parseRoles(claims.get(CLAIM_ROLES));
        return new VerifiedAccessToken(username, roles);
    }

    private List<String> parseRoles(Object rolesClaim) {
        if (rolesClaim == null) {
            return List.of();
        }

        if (rolesClaim instanceof Collection<?> collection) {
            List<String> roles = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof String role && StringUtils.hasText(role)) {
                    roles.add(role.trim());
                }
            }
            return roles;
        }

        if (rolesClaim instanceof String roleString) {
            if (!StringUtils.hasText(roleString)) {
                return List.of();
            }
            String[] parts = roleString.split(",");
            List<String> roles = new ArrayList<>();
            for (String part : parts) {
                if (StringUtils.hasText(part)) {
                    roles.add(part.trim());
                }
            }
            return roles;
        }

        return List.of();
    }
}
