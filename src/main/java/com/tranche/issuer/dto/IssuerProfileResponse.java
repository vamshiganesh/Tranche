package com.tranche.issuer.dto;

import java.util.UUID;

public record IssuerProfileResponse(
        Long id,
        String companyName,
        String registrationNumber,
        UUID userId
) {
}
