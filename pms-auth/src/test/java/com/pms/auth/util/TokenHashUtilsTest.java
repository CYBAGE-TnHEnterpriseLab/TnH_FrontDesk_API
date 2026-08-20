package com.pms.auth.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TokenHashUtilsTest {

    @Test
    void sha256ShouldBeDeterministic() {
        String hash1 = TokenHashUtils.sha256("refresh-token-value");
        String hash2 = TokenHashUtils.sha256("refresh-token-value");

        assertEquals(hash1, hash2);
    }

    @Test
    void sha256ShouldChangeForDifferentValues() {
        String hash1 = TokenHashUtils.sha256("refresh-token-value");
        String hash2 = TokenHashUtils.sha256("different-token-value");

        assertNotEquals(hash1, hash2);
    }
}

