package com.tranche.issuer.service;

import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.dto.IssuerProfileResponse;

final class IssuerMapper {

    private IssuerMapper() {
    }

    static IssuerProfileResponse toResponse(Issuer issuer) {
        return new IssuerProfileResponse(
                issuer.getId(),
                issuer.getCompanyName(),
                issuer.getRegistrationNumber(),
                issuer.getUser().getPublicId()
        );
    }
}
