package Policy_Management.Policy.security;

import com.pms.security.jwt.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAccessTokenValidator jwtAccessTokenValidator;

    public AuthFilter(JwtAccessTokenValidator jwtAccessTokenValidator) {
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

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "AuthFilter resolved authorities: method={}, path={}, user={}, rawRoles={}, mappedAuthorities={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        verifiedToken.username(),
                        verifiedToken.roles(),
                        authorities
                );
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    verifiedToken.username(),
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            RequestUserContext.setUsername(verifiedToken.username());
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            RequestUserContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/error")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/uploads/");
    }

    private boolean isPreflightRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String sanitizedMessage = message == null ? "Unauthorized" : message.replace("\"", "\\\"");
        response.getWriter().write("{\"status\":\"fail\",\"message\":\"" + sanitizedMessage + "\",\"data\":null}");
    }
}


