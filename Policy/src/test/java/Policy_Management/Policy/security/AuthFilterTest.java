package Policy_Management.Policy.security;

import com.pms.security.jwt.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestUserContext.clear();
    }

    @Test
    void doFilter_allowsPreflightRequest() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/policies/getAllPolicies");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_allowsPublicPaths() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_allowsErrorPath() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_allowsUploadsPath() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/file.txt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_returns401_whenAuthorizationHeaderMissing() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/policies/getAllPolicies");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing or invalid Authorization header"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_returns401_whenTokenIsInvalid() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        when(validator.validateAccessToken("bad-token")).thenReturn(Optional.empty());
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/policies/getAllPolicies");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired access token"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_setsAuthenticationAndClearsContextAfterChain() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AccessTokenVerifier.VerifiedAccessToken token = new AccessTokenVerifier.VerifiedAccessToken("alice",
                Set.of("admin", "ROLE_user"));
        when(validator.validateAccessToken("good-token")).thenReturn(Optional.of(token));
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/policies/getAllPolicies");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Authentication> authInChain = new AtomicReference<>();
        AtomicReference<String> usernameInChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            authInChain.set(SecurityContextHolder.getContext().getAuthentication());
            usernameInChain.set(RequestUserContext.getUsername());
        };

        filter.doFilter(request, response, chain);

        assertNotNull(authInChain.get());
        assertEquals("alice", authInChain.get().getName());
        assertNotNull(authInChain.get().getAuthorities());
        assertTrue(authInChain.get().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authInChain.get().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertEquals("alice", usernameInChain.get());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(RequestUserContext.getUsername());
    }

    @Test
    void doFilter_escapesQuotesInUnauthorizedMessage() throws ServletException, IOException {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        when(validator.validateAccessToken("bad\"token")).thenReturn(Optional.empty());
        AuthFilter filter = new AuthFilter(validator);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/policies/getAllPolicies");
        request.addHeader("Authorization", "Bearer bad\"token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired access token"));
    }

    @Test
    void privateHelpers_coverRemainingBranches() throws Exception {
        JwtAccessTokenValidator validator = mock(JwtAccessTokenValidator.class);
        AuthFilter filter = new AuthFilter(validator);

        Method isPublicPath = AuthFilter.class.getDeclaredMethod("isPublicPath", String.class);
        isPublicPath.setAccessible(true);
        assertTrue((Boolean) isPublicPath.invoke(filter, "/v3/api-docs/index"));
        assertFalse((Boolean) isPublicPath.invoke(filter, "/api/secure"));

        Method isPreflightRequest = AuthFilter.class.getDeclaredMethod("isPreflightRequest",
                jakarta.servlet.http.HttpServletRequest.class);
        isPreflightRequest.setAccessible(true);
        MockHttpServletRequest optionsRequest = new MockHttpServletRequest("OPTIONS", "/x");
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/x");
        assertTrue((Boolean) isPreflightRequest.invoke(filter, optionsRequest));
        assertFalse((Boolean) isPreflightRequest.invoke(filter, getRequest));

        Method writeUnauthorized = AuthFilter.class.getDeclaredMethod("writeUnauthorized",
                jakarta.servlet.http.HttpServletResponse.class, String.class);
        writeUnauthorized.setAccessible(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        writeUnauthorized.invoke(filter, response, (String) null);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }
}
