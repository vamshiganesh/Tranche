package com.tranche.auth.dto;

import com.tranche.common.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        String fullName,
        Instant createdAt
) {
}
