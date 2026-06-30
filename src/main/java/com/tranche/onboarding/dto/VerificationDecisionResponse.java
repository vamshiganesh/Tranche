package com.tranche.onboarding.dto;

import com.tranche.common.domain.VerificationStatus;

import java.util.UUID;

public record VerificationDecisionResponse(
        UUID userId,
        VerificationStatus status
) {
}
