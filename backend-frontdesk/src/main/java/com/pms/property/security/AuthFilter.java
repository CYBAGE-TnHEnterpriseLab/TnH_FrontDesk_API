package com.pms.property.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter extends OncePerRequestFilter {

    private static final String HARD_CODED_USERNAME = "admin";

    private final ObjectMapper objectMapper;

    public AuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            RequestUserContext.setUsername(HARD_CODED_USERNAME);
            filterChain.doFilter(request, response);
        } finally {
            RequestUserContext.clear();
        }
    }
}

