package com.tranche.auth.dto;

import com.tranche.common.domain.Role;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        Role role,
        String fullName,
        BigDecimal walletBalance
) {
}
