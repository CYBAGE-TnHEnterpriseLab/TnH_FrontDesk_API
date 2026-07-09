package com.frontdesk.pms.rate_management.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @Mock
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAllowPublicEndpointWithoutToken() throws ServletException, IOException {
        AuthFilter authFilter = new AuthFilter(jwtAccessTokenValidator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        authFilter.doFilter(request, response, chain);

        assertTrue(chainInvoked.get());
        verifyNoInteractions(jwtAccessTokenValidator);
    }

    @Test
    void doFilterInternal_shouldReturn401WhenAuthorizationHeaderMissing() throws ServletException, IOException {
        AuthFilter authFilter = new AuthFilter(jwtAccessTokenValidator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        authFilter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"success\":false,\"data\":null,\"message\":\"Invalid or expired access token\"}", response.getContentAsString());
        assertTrue(!chainInvoked.get());
    }

    @Test
    void doFilterInternal_shouldReturn401WhenTokenValidationFails() throws ServletException, IOException {
        AuthFilter authFilter = new AuthFilter(jwtAccessTokenValidator);

        String token = "invalid-token";
        when(jwtAccessTokenValidator.validate(token)).thenThrow(new JwtException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure/ping");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        authFilter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"success\":false,\"data\":null,\"message\":\"Invalid or expired access token\"}", response.getContentAsString());
        assertTrue(!chainInvoked.get());
    }

    @Test
    void doFilterInternal_shouldSetSecurityContextForValidAccessToken() throws ServletException, IOException {
        AuthFilter authFilter = new AuthFilter(jwtAccessTokenValidator);

        String token = "valid-token";
        when(jwtAccessTokenValidator.validate(token))
                .thenReturn(new AccessTokenVerifier.VerifiedAccessToken("admin-user", List.of("ADMIN")));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure/ping");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals("admin-user", authentication.getName());
            assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
            chainInvoked.set(true);
        };

        authFilter.doFilter(request, response, chain);

        assertTrue(chainInvoked.get());
    }
}
