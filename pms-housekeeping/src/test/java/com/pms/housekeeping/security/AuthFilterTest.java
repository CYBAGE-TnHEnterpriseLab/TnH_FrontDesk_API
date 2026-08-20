package com.pms.housekeeping.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.security.jwt.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    private JwtAccessTokenValidator validator;
    private AuthFilter filter;

    @BeforeEach
    void setUp() {
        validator = mock(JwtAccessTokenValidator.class);
        filter = new AuthFilter(new ObjectMapper(), validator);
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
        assertThat(response.getStatus()).isEqualTo(200);
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
        assertThat(response.getStatus()).isEqualTo(200);
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
        when(validator.validateAccessToken("bad-token")).thenReturn(Optional.empty());

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

        when(validator.validateAccessToken("good-token")).thenReturn(Optional.of(
                new AccessTokenVerifier.VerifiedAccessToken("alice", Set.of("admin", "ROLE_USER"))
        ));
        doAnswer(invocation -> {
            authenticationInsideChain.set(SecurityContextHolder.getContext().getAuthentication());
            assertThat(RequestUserContext.getUsername()).isEqualTo("alice");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertThat(authenticationInsideChain.get()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authenticationInsideChain.get().getName()).isEqualTo("alice");
        assertThat(authenticationInsideChain.get().getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(RequestUserContext.getUsername()).isNull();
    }

    @Test
    void doFilter_shouldReturnUnauthorizedWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");

        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("Missing or invalid Authorization header");

        verify(validator, never()).validateAccessToken(any());
    }

    @Test
    void doFilter_shouldBypassApiDocsPath() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/v3/api-docs");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        verify(validator, never()).validateAccessToken(any());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(RequestUserContext.getUsername()).isNull();
    }

    @Test
    void doFilter_shouldBypassNestedApiDocsPath() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/v3/api-docs/swagger-config");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        verify(validator, never()).validateAccessToken(any());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_shouldBypassErrorPath() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/error");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        verify(validator, never()).validateAccessToken(any());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_shouldClearContextWhenFilterChainThrowsException() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer good-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(validator.validateAccessToken("good-token"))
                .thenReturn(Optional.of(
                        new AccessTokenVerifier.VerifiedAccessToken(
                                "alice",
                                Set.of("ADMIN")
                        )
                ));

        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNotNull();

            assertThat(RequestUserContext.getUsername())
                    .isEqualTo("alice");

            throw new ServletException("Test exception");
        }).when(chain).doFilter(request, response);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.doFilter(request, response, chain)
                )
                .isInstanceOf(ServletException.class)
                .hasMessage("Test exception");

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();

        assertThat(RequestUserContext.getUsername())
                .isNull();
    }

    @Test
    void doFilter_shouldReturnUnauthorizedWhenBearerTokenIsEmpty() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/housekeeping/rooms");

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer "
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        when(validator.validateAccessToken(""))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());

        assertThat(response.getStatus()).isEqualTo(401);

        assertThat(response.getContentAsString())
                .contains("Invalid or expired access token");

        verify(validator).validateAccessToken("");
    }
}


