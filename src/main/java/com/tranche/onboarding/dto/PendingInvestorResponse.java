package com.tranche.onboarding.dto;

import com.tranche.common.domain.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record PendingInvestorResponse(
        UUID userId,
        String email,
        String fullName,
        VerificationStatus kycStatus,
        Instant registeredAt
) {
}
