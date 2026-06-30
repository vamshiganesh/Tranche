package com.tranche.investor.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class InvestorKycService {

    private final InvestorProfileRepository investorProfileRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public InvestorKycService(
            InvestorProfileRepository investorProfileRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.investorProfileRepository = investorProfileRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void resubmitKyc(UserPrincipal principal) {
        InvestorProfile profile = investorProfileRepository.findByUser_PublicId(principal.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));

        if (profile.getKycStatus() != VerificationStatus.REJECTED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "KYC can only be resubmitted after rejection"
            );
        }

        VerificationStatus before = profile.getKycStatus();
        profile.setKycStatus(VerificationStatus.PENDING);
        investorProfileRepository.save(profile);

        User actor = userRepository.getReferenceById(principal.getId());
        auditService.log(
                actor,
                AuditActorRole.INVESTOR,
                AuditActions.INVESTOR_KYC_RESUBMITTED,
                AuditEntityTypes.INVESTOR_PROFILE,
                profile.getId(),
                Map.of("kycStatus", before.name()),
                Map.of("kycStatus", VerificationStatus.PENDING.name())
        );
    }
}
