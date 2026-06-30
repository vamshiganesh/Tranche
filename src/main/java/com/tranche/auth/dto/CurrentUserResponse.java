package com.tranche.auth.dto;

import com.tranche.common.domain.Role;
import com.tranche.common.domain.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        Role role,
        String fullName,
        boolean emailVerified,
        BigDecimal walletBalance,
        VerificationStatus kycStatus,
        boolean hasIssuerProfile,
        VerificationStatus issuerVerificationStatus
) {
}
