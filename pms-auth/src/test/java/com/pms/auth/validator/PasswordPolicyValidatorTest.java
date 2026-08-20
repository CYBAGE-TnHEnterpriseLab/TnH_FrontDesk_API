package com.pms.auth.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyValidatorTest {

    @Test
    void isStrongShouldReturnTrueForValidPassword() {
        assertTrue(PasswordPolicyValidator.isStrong("StrongPass1"));
    }

    @Test
    void isStrongShouldReturnFalseForWeakPassword() {
        assertFalse(PasswordPolicyValidator.isStrong("weak"));
    }
}

