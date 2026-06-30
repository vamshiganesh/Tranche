package com.tranche.onboarding.controller;

import com.tranche.common.security.SecurityUtils;
import com.tranche.onboarding.dto.PendingInvestorResponse;
import com.tranche.onboarding.dto.PendingIssuerResponse;
import com.tranche.onboarding.dto.VerificationDecisionResponse;
import com.tranche.onboarding.service.OnboardingAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/onboarding")
@PreAuthorize("hasRole('ADMIN')")
public class OnboardingAdminController {

    private final OnboardingAdminService onboardingAdminService;

    public OnboardingAdminController(OnboardingAdminService onboardingAdminService) {
        this.onboardingAdminService = onboardingAdminService;
    }

    @GetMapping("/investors")
    public List<PendingInvestorResponse> listPendingInvestors() {
        return onboardingAdminService.listPendingInvestors();
    }

    @GetMapping("/issuers")
    public List<PendingIssuerResponse> listPendingIssuers() {
        return onboardingAdminService.listPendingIssuers();
    }

    @PostMapping("/investors/{userId}/approve")
    public VerificationDecisionResponse approveInvestor(@PathVariable UUID userId) {
        return onboardingAdminService.approveInvestorKyc(userId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/investors/{userId}/reject")
    public VerificationDecisionResponse rejectInvestor(@PathVariable UUID userId) {
        return onboardingAdminService.rejectInvestorKyc(userId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/issuers/{userId}/approve")
    public VerificationDecisionResponse approveIssuer(@PathVariable UUID userId) {
        return onboardingAdminService.approveIssuerKyb(userId, SecurityUtils.requireCurrentUser());
    }

    @PostMapping("/issuers/{userId}/reject")
    public VerificationDecisionResponse rejectIssuer(@PathVariable UUID userId) {
        return onboardingAdminService.rejectIssuerKyb(userId, SecurityUtils.requireCurrentUser());
    }
}
