package com.folio.billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAccessTokenValidator jwtAccessTokenValidator;
    private final List<RequestMatcher> publicPathMatchers;

    public AuthFilter(
            JwtAccessTokenValidator jwtAccessTokenValidator,
            JwtSecurityProperties securityProperties
    ) {
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        this.publicPathMatchers = securityProperties.getPublicPaths().stream()
            .map(matcherBuilder::matcher)
                .map(RequestMatcher.class::cast)
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicPathMatchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            SecurityContextHolder.getContext().setAuthentication(jwtAccessTokenValidator.validate(token));
            filterChain.doFilter(request, response);
        } catch (JwtValidationException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, request.getRequestURI());
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String path) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{" +
                "\"success\":false," +
                "\"message\":\"Invalid or expired access token\"," +
                "\"path\":\"" + escape(path) + "\"," +
                "\"timestamp\":\"" + Instant.now() + "\"" +
                "}");
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
