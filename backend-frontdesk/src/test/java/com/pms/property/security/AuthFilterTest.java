package com.pms.property.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.security.jwt.AccessTokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

class AuthFilterTest {

	private final JwtAccessTokenValidator jwtAccessTokenValidator = org.mockito.Mockito.mock(JwtAccessTokenValidator.class);
	private final AuthFilter authFilter = new AuthFilter(new ObjectMapper(), jwtAccessTokenValidator);

	@AfterEach
	void tearDown() {
		RequestUserContext.clear();
	}

	@Test
	void shouldAllowAdminTokenAndPopulateUserContext() throws ServletException, IOException {
		when(jwtAccessTokenValidator.validateAccessToken("valid-token"))
			.thenReturn(java.util.Optional.of(new AccessTokenVerifier.VerifiedAccessToken("admin", Set.of("ADMIN"))));

		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		request.setRequestURI("/api/property/drafts");
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenUsername = new AtomicReference<>();
		FilterChain filterChain = (req, res) -> seenUsername.set(RequestUserContext.getUsername());

		authFilter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus());
		assertEquals("admin", seenUsername.get());
		assertNull(RequestUserContext.getUsername());
	}

	@Test
	void shouldRejectNonAdminTokenWithForbidden() throws ServletException, IOException {
		when(jwtAccessTokenValidator.validateAccessToken("valid-token"))
			.thenReturn(java.util.Optional.of(new AccessTokenVerifier.VerifiedAccessToken("operator", Set.of("USER"))));

		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		request.setRequestURI("/api/property/drafts");
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		authFilter.doFilter(request, response, (req, res) -> {
			throw new AssertionError("Filter chain should not run for non-admin");
		});

		assertEquals(403, response.getStatus());
	}

	@Test
	void shouldRejectMissingAuthorizationHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		request.setRequestURI("/api/property/drafts");
		MockHttpServletResponse response = new MockHttpServletResponse();

		authFilter.doFilter(request, response, (req, res) -> {
			throw new AssertionError("Filter chain should not run without token");
		});

		assertEquals(401, response.getStatus());
		verifyNoInteractions(jwtAccessTokenValidator);
	}

	@Test
	void shouldSkipPublicPathWithoutToken() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		request.setRequestURI("/v3/api-docs");
		MockHttpServletResponse response = new MockHttpServletResponse();

		authFilter.doFilter(request, response, (req, res) -> {
			// no-op
		});

		assertEquals(200, response.getStatus());
		verifyNoInteractions(jwtAccessTokenValidator);
	}

	@Test
	void shouldRejectInvalidToken() throws ServletException, IOException {
		when(jwtAccessTokenValidator.validateAccessToken(anyString())).thenReturn(java.util.Optional.empty());

		MockHttpServletRequest request = new MockHttpServletRequest(new MockServletContext());
		request.setRequestURI("/api/property/drafts");
		request.addHeader("Authorization", "Bearer invalid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		authFilter.doFilter(request, response, (req, res) -> {
			throw new AssertionError("Filter chain should not run for invalid token");
		});

		assertEquals(401, response.getStatus());
	}
}


