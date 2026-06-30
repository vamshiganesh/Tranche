package com.tranche.auth.dto;

import com.tranche.auth.validation.PasswordPolicy;
import com.tranche.common.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$", message = PasswordPolicy.MESSAGE)
        String password,
        @NotNull Role role,
        @NotBlank @Size(max = 255) String fullName
) {
}
