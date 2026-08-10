package Policy_Management.Policy.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtAccessTokenValidatorTest {

    @Test
    void validateAccessToken_returnsEmpty_forInvalidToken() {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator(
                "this-is-a-long-enough-secret-for-tests-123456");

        assertTrue(validator.validateAccessToken("invalid.token.value").isEmpty());
    }
}
