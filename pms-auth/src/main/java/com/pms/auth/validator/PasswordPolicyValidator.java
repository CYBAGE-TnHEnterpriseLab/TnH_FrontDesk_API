package com.pms.auth.validator;

public final class PasswordPolicyValidator {

    private PasswordPolicyValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUpper && hasLower && hasDigit;
    }
}

