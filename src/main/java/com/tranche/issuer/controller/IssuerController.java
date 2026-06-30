package com.tranche.issuer.controller;

import com.tranche.common.security.SecurityUtils;
import com.tranche.issuer.dto.CreateIssuerProfileRequest;
import com.tranche.issuer.dto.IssuerProfileResponse;
import com.tranche.issuer.service.IssuerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/issuers")
public class IssuerController {

    private final IssuerProfileService issuerProfileService;

    public IssuerController(IssuerProfileService issuerProfileService) {
        this.issuerProfileService = issuerProfileService;
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('ISSUER')")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuerProfileResponse createProfile(@Valid @RequestBody CreateIssuerProfileRequest request) {
        return issuerProfileService.createProfile(SecurityUtils.requireCurrentPublicId(), request);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('ISSUER')")
    public IssuerProfileResponse resubmitProfilePut(@Valid @RequestBody CreateIssuerProfileRequest request) {
        return issuerProfileService.resubmitProfile(SecurityUtils.requireCurrentPublicId(), request);
    }

    @PostMapping("/profile/resubmit")
    @PreAuthorize("hasRole('ISSUER')")
    public IssuerProfileResponse resubmitProfile(@Valid @RequestBody CreateIssuerProfileRequest request) {
        return issuerProfileService.resubmitProfile(SecurityUtils.requireCurrentPublicId(), request);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ISSUER')")
    public IssuerProfileResponse getProfile() {
        return issuerProfileService.getProfile(SecurityUtils.requireCurrentPublicId());
    }
}
