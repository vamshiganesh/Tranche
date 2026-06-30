package com.tranche.auth.validation;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String MESSAGE =
            "Password must be at least 8 characters and include uppercase, lowercase, number, and symbol";

    private static final Pattern COMPLEXITY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$"
    );

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && COMPLEXITY.matcher(password).matches();
    }
}
