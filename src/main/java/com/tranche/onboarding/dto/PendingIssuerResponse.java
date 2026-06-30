package com.tranche.onboarding.dto;

import com.tranche.common.domain.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record PendingIssuerResponse(
        UUID userId,
        String email,
        String fullName,
        String companyName,
        String registrationNumber,
        VerificationStatus verificationStatus,
        Instant registeredAt
) {
}
