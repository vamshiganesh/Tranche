package com.tranche.issuer.dto;

import com.tranche.common.domain.VerificationStatus;

import java.util.UUID;

public record IssuerProfileResponse(
        Long id,
        String companyName,
        String registrationNumber,
        UUID userId,
        VerificationStatus verificationStatus
) {
}
