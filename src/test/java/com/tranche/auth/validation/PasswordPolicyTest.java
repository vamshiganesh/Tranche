package com.tranche.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void acceptsComplexPassword() {
        assertThat(PasswordPolicy.isValid("Password123!")).isTrue();
    }

    @Test
    void rejectsMissingSymbol() {
        assertThat(PasswordPolicy.isValid("Password123")).isFalse();
    }

    @Test
    void rejectsMissingUppercase() {
        assertThat(PasswordPolicy.isValid("password123!")).isFalse();
    }

    @Test
    void rejectsTooShort() {
        assertThat(PasswordPolicy.isValid("Pass1!")).isFalse();
    }
}
