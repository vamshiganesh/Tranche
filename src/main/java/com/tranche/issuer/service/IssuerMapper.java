package com.tranche.issuer.service;

import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.dto.IssuerProfileResponse;

import java.util.UUID;

final class IssuerMapper {

    private IssuerMapper() {
    }

    static IssuerProfileResponse toResponse(Issuer issuer, UUID userPublicId) {
        return new IssuerProfileResponse(
                issuer.getId(),
                issuer.getCompanyName(),
                issuer.getRegistrationNumber(),
                userPublicId,
                issuer.getVerificationStatus()
        );
    }
}
