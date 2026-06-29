package com.tranche.issuer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIssuerProfileRequest(
        @NotBlank @Size(max = 255) String companyName,
        @Size(max = 100) String registrationNumber
) {
}
