package com.tranche.onboarding.service;

import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.domain.AuditEntityTypes;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.VerificationStatus;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import com.tranche.onboarding.dto.PendingInvestorResponse;
import com.tranche.onboarding.dto.PendingIssuerResponse;
import com.tranche.onboarding.dto.VerificationDecisionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OnboardingAdminService {

    private final InvestorProfileRepository investorProfileRepository;
    private final IssuerRepository issuerRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public OnboardingAdminService(
            InvestorProfileRepository investorProfileRepository,
            IssuerRepository issuerRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.investorProfileRepository = investorProfileRepository;
        this.issuerRepository = issuerRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PendingInvestorResponse> listPendingInvestors() {
        return investorProfileRepository.findByKycStatus(VerificationStatus.PENDING).stream()
                .map(this::toPendingInvestor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingIssuerResponse> listPendingIssuers() {
        return issuerRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
                .map(this::toPendingIssuer)
                .toList();
    }

    @Transactional
    public VerificationDecisionResponse approveInvestorKyc(UUID userPublicId, UserPrincipal admin) {
        return updateInvestorKyc(userPublicId, admin, VerificationStatus.APPROVED);
    }

    @Transactional
    public VerificationDecisionResponse rejectInvestorKyc(UUID userPublicId, UserPrincipal admin) {
        return updateInvestorKyc(userPublicId, admin, VerificationStatus.REJECTED);
    }

    @Transactional
    public VerificationDecisionResponse approveIssuerKyb(UUID userPublicId, UserPrincipal admin) {
        return updateIssuerKyb(userPublicId, admin, VerificationStatus.APPROVED);
    }

    @Transactional
    public VerificationDecisionResponse rejectIssuerKyb(UUID userPublicId, UserPrincipal admin) {
        return updateIssuerKyb(userPublicId, admin, VerificationStatus.REJECTED);
    }

    private VerificationDecisionResponse updateInvestorKyc(
            UUID userPublicId,
            UserPrincipal admin,
            VerificationStatus status
    ) {
        InvestorProfile profile = investorProfileRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));

        VerificationStatus before = profile.getKycStatus();
        if (before != VerificationStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Investor KYC is not pending review"
            );
        }

        profile.setKycStatus(status);
        investorProfileRepository.save(profile);

        User actor = userRepository.getReferenceById(admin.getId());
        auditService.log(
                actor,
                AuditActorRole.ADMIN,
                status == VerificationStatus.APPROVED
                        ? AuditActions.INVESTOR_KYC_APPROVED
                        : AuditActions.INVESTOR_KYC_REJECTED,
                AuditEntityTypes.INVESTOR_PROFILE,
                profile.getId(),
                Map.of("kycStatus", before.name()),
                Map.of("kycStatus", status.name())
        );

        return new VerificationDecisionResponse(userPublicId, status);
    }

    private VerificationDecisionResponse updateIssuerKyb(
            UUID userPublicId,
            UserPrincipal admin,
            VerificationStatus status
    ) {
        Issuer issuer = issuerRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));

        VerificationStatus before = issuer.getVerificationStatus();
        if (before != VerificationStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Issuer KYB is not pending review"
            );
        }

        issuer.setVerificationStatus(status);
        issuerRepository.save(issuer);

        User actor = userRepository.getReferenceById(admin.getId());
        auditService.log(
                actor,
                AuditActorRole.ADMIN,
                status == VerificationStatus.APPROVED
                        ? AuditActions.ISSUER_KYB_APPROVED
                        : AuditActions.ISSUER_KYB_REJECTED,
                AuditEntityTypes.ISSUER,
                issuer.getId(),
                Map.of("verificationStatus", before.name()),
                Map.of("verificationStatus", status.name())
        );

        return new VerificationDecisionResponse(userPublicId, status);
    }

    private PendingInvestorResponse toPendingInvestor(InvestorProfile profile) {
        User user = profile.getUser();
        return new PendingInvestorResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getFullName(),
                profile.getKycStatus(),
                user.getCreatedAt()
        );
    }

    private PendingIssuerResponse toPendingIssuer(Issuer issuer) {
        User user = issuer.getUser();
        return new PendingIssuerResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getFullName(),
                issuer.getCompanyName(),
                issuer.getRegistrationNumber(),
                issuer.getVerificationStatus(),
                user.getCreatedAt()
        );
    }
}
