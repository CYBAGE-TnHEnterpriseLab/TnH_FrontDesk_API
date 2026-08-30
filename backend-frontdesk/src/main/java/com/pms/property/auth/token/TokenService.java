package com.pms.property.auth.token;

import com.pms.property.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final byte[] secret;
    private final long expirySeconds;

    public TokenService(
        @Value("${app.auth.token-secret:change-this-demo-secret}") String tokenSecret,
        @Value("${app.auth.token-expiry-seconds:28800}") long expirySeconds
    ) {
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.expirySeconds = expirySeconds;
    }

    public String issueToken(String username) {
        long expiresAt = Instant.now().getEpochSecond() + expirySeconds;
        String payload = username + "|" + expiresAt;
        String signature = sign(payload);
        return base64Url(payload) + "." + base64Url(signature);
    }

    public Optional<AuthPrincipal> parse(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String payload;
        String providedSignature;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            providedSignature = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        String expectedSignature = sign(payload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), providedSignature.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        String[] payloadParts = payload.split("\\|", 2);
        if (payloadParts.length != 2) {
            return Optional.empty();
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }

        return Optional.of(new AuthPrincipal(payloadParts[0], expiresAt));
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            byte[] signedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception ex) {
            throw new BadRequestException("Token signing failed");
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

