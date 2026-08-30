package com.pms.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.guestlisting.exception.GlobalExceptionHandler;
import com.pms.reservation.controller.ReservationController;
import com.pms.common.config.SecurityAutoConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ReservationController.class,
        properties = {
                "security.jwt.secret=abcdefghijklmnopqrstuvwxyz123456"
        }
)
@Import({
        SecurityConfig.class,
        SecurityAutoConfiguration.class,
        GlobalExceptionHandler.class
})
class JwtSecurityIntegrationTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final String WRONG_SECRET = "wrongsecretwrongsecretwrongsecret12345";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.pms.reservation.service.ReservationBookingService reservationBookingService;

    @Test
    void protectedEndpointShouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/payment-modes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
    }

    @Test
    void protectedEndpointShouldAllowValidAccessTokenAndRejectInvalidTypeToken() throws Exception {
        String accessToken = buildToken("access", List.of("ADMIN"));
        String refreshToken = buildToken("refresh", List.of("ADMIN"));

        mockMvc.perform(get("/api/v1/reservations/payment-modes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/reservations/payment-modes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void protectedEndpointShouldReturnForbiddenForValidTokenWithoutAdminRole() throws Exception {
        String userToken = buildToken("access", List.of("USER"));

        mockMvc.perform(get("/api/v1/reservations/payment-modes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedForInvalidSignature() throws Exception {
        String token = Jwts.builder()
                .subject("admin.user")
                .claim("typ", "access")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(1800)))
                .signWith(Keys.hmacShaKeyFor(WRONG_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/reservations/payment-modes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    private String buildToken(String type, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("admin.user")
                .claim("typ", type)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(key)
                .compact();
    }
}
