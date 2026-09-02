package com.pms.common.security;

import com.pms.common.config.JwtProperties;
import com.pms.common.security.RequestUserContext;
import com.pms.common.utils.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private AccessTokenVerifier verifier;
    private JwtAuthenticationFilter filter;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        verifier = mock(AccessTokenVerifier.class);
        properties = new JwtProperties();
        properties.setSecret("pms-auth-super-secret-key-change-me-in-prod-2026");
        properties.setPublicPaths(List.of("/error", "/swagger-ui/**", "/v3/api-docs/**"));
        filter = new JwtAuthenticationFilter(verifier, properties);
    }

    @AfterEach
    void tearDown() {
        RequestUserContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldBypassPublicPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(RequestUserContext.getUsername()).isNull();
    }

    @Test
    void doFilter_shouldBypassPreflightRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/housekeeping/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing or invalid Authorization header");
    }

    @Test
    void doFilter_shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(verifier.verify("bad-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired access token");
    }

    @Test
    void doFilter_shouldSetAuthenticationAndClearContextAfterSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        AtomicReference<Authentication> authenticationInsideChain = new AtomicReference<>();

        when(verifier.verify("good-token")).thenReturn(Optional.of(
                new AccessTokenVerifier.VerifiedAccessToken(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"), "alice", Set.of("admin", "ROLE_USER"))));
        doAnswer(invocation -> {
            authenticationInsideChain.set(SecurityContextHolder.getContext().getAuthentication());
            assertThat(RequestUserContext.getUsername()).isEqualTo("alice");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(authenticationInsideChain.get()).isNotNull();
        assertThat(authenticationInsideChain.get().getName()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(authenticationInsideChain.get().getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(RequestUserContext.getUsername()).isNull();
    }
}
