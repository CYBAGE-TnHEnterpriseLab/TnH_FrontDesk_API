package com.pms.housekeeping.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.security.jwt.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;
    private final JwtAccessTokenValidator jwtAccessTokenValidator;

    public AuthFilter(ObjectMapper objectMapper, JwtAccessTokenValidator jwtAccessTokenValidator) {
        this.objectMapper = objectMapper;
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (isPreflightRequest(request) || isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        AccessTokenVerifier.VerifiedAccessToken verifiedToken = jwtAccessTokenValidator
            .validateAccessToken(token)
            .orElse(null);
        if (verifiedToken == null) {
            writeUnauthorized(response, "Invalid or expired access token");
            return;
        }

        try {
            List<SimpleGrantedAuthority> authorities = verifiedToken.roles().stream()
                .map(String::toUpperCase)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                verifiedToken.username(),
                null,
                authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/error")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }

    private boolean isPreflightRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
            java.util.Map.of("success", false, "message", message)
        ));
    }
}


