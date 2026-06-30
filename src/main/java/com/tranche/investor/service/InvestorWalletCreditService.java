package com.tranche.investor.service;

import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.domain.AuditEntityTypes;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.config.InvestorProperties;
import com.tranche.common.domain.Role;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.dto.DemoCreditResponse;
import com.tranche.investor.repository.InvestorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class InvestorWalletCreditService {

    private final InvestorProfileRepository investorProfileRepository;
    private final InvestorProperties investorProperties;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public InvestorWalletCreditService(
            InvestorProfileRepository investorProfileRepository,
            InvestorProperties investorProperties,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.investorProfileRepository = investorProfileRepository;
        this.investorProperties = investorProperties;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DemoCreditResponse applyDemoCredit(UserPrincipal principal) {
        if (principal.getRole() != Role.INVESTOR) {
            throw new ForbiddenException("Only investors can request demo funds");
        }
        if (!investorProperties.demoCreditEnabled()) {
            throw new BusinessException(
                    ErrorCode.DEMO_CREDIT_NOT_AVAILABLE,
                    "Demo wallet credit is not available in this environment"
            );
        }

        InvestorProfile profile = investorProfileRepository.findByUserIdForUpdate(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));

        BigDecimal creditAmount = investorProperties.demoCreditAmount();
        BigDecimal before = profile.getWalletBalance();
        profile.setWalletBalance(before.add(creditAmount));
        investorProfileRepository.save(profile);

        User actor = userRepository.getReferenceById(principal.getId());
        auditService.log(
                actor,
                AuditActorRole.INVESTOR,
                AuditActions.DEMO_WALLET_CREDITED,
                AuditEntityTypes.INVESTOR_PROFILE,
                profile.getId(),
                Map.of("walletBalance", before),
                Map.of("walletBalance", profile.getWalletBalance(), "creditedAmount", creditAmount)
        );

        return new DemoCreditResponse(creditAmount, profile.getWalletBalance());
    }
}
