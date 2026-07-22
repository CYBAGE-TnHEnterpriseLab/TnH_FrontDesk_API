package com.pms.property.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtAccessTokenValidatorTest {

	private static final String SECRET = "test-jwt-secret-key-min-32-bytes-12345";

	@Test
	void shouldExtractUsernameAndRolesFromValidAccessToken() {
		JwtAccessTokenValidator validator = new JwtAccessTokenValidator(SECRET);
		String token = buildToken("admin", "access", List.of("ADMIN"));

		var validatedToken = validator.validateAccessToken(token).orElseThrow();

		assertEquals("admin", validatedToken.username());
		assertEquals(List.of("ADMIN"), List.copyOf(validatedToken.roles()));
	}

	@Test
	void shouldRejectRefreshTokenForAccessValidation() {
		JwtAccessTokenValidator validator = new JwtAccessTokenValidator(SECRET);
		String token = buildToken("admin", "refresh", List.of("ADMIN"));

		assertTrue(validator.validateAccessToken(token).isEmpty());
	}

	private String buildToken(String subject, String tokenType, List<String> roles) {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();

		return Jwts.builder()
			.subject(subject)
			.claim("typ", tokenType)
			.claim("roles", roles)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(300)))
			.signWith(key)
			.compact();
	}
}
