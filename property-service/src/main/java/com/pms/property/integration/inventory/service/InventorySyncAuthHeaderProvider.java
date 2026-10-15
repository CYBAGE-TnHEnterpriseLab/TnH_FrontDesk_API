package com.pms.property.integration.inventory.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class InventorySyncAuthHeaderProvider {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final String fallbackServiceToken;

    public InventorySyncAuthHeaderProvider(
        @Value("${inventory.sync.service-token:}") String fallbackServiceToken
    ) {
        this.fallbackServiceToken = fallbackServiceToken == null ? "" : fallbackServiceToken.trim();
    }

    public Optional<String> resolveAuthorizationHeader() {
        String inboundHeader = inboundAuthorizationHeader();
        if (inboundHeader != null && !inboundHeader.isBlank()) {
            return Optional.of(inboundHeader);
        }
        if (!fallbackServiceToken.isBlank()) {
            return Optional.of(BEARER_PREFIX + fallbackServiceToken);
        }
        return Optional.empty();
    }

    public Optional<String> resolveAuthorizationHeader(String explicitHeader) {
        if (explicitHeader != null && !explicitHeader.isBlank()) {
            return Optional.of(explicitHeader);
        }
        return resolveAuthorizationHeader();
    }

    private String inboundAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        HttpServletRequest request = servletAttributes.getRequest();
        String header = request.getHeader(AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        return header;
    }
}

