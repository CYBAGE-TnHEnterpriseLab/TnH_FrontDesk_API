package com.folio.billing.security;

import java.time.Instant;
import java.util.List;

public record VerifiedAccessToken(
        String username,
        List<String> roles,
        Instant expiresAt,
        String tokenId
) {
}
