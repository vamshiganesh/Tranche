package com.tranche.issuer.service;

import com.tranche.audit.domain.AuditActions;
import com.tranche.audit.domain.AuditActorRole;
import com.tranche.audit.domain.AuditEntityTypes;
import com.tranche.audit.service.AuditService;
import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.domain.VerificationStatus;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ConflictException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.dto.CreateIssuerProfileRequest;
import com.tranche.issuer.dto.IssuerProfileResponse;
import com.tranche.issuer.repository.IssuerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class IssuerProfileService {

    private final IssuerRepository issuerRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public IssuerProfileService(
            IssuerRepository issuerRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.issuerRepository = issuerRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public IssuerProfileResponse createProfile(UUID userPublicId, CreateIssuerProfileRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ISSUER) {
            throw new ForbiddenException("Only issuer accounts can create an issuer profile");
        }
        if (issuerRepository.existsByUser_Id(user.getId())) {
            throw new ConflictException("Issuer profile already exists");
        }

        Issuer issuer = new Issuer();
        issuer.setUser(user);
        issuer.setCompanyName(request.companyName().trim());
        issuer.setRegistrationNumber(request.registrationNumber());
        issuerRepository.save(issuer);

        return IssuerMapper.toResponse(issuer, userPublicId);
    }

    @Transactional(readOnly = true)
    public IssuerProfileResponse getProfile(UUID userPublicId) {
        Issuer issuer = issuerRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));
        return IssuerMapper.toResponse(issuer, userPublicId);
    }
}
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ISSUER) {
            throw new ForbiddenException("Only issuer accounts can update an issuer profile");
        }

        Issuer issuer = issuerRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));

        if (issuer.getVerificationStatus() != VerificationStatus.REJECTED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Company profile can only be updated after rejection"
            );
        }

        VerificationStatus before = issuer.getVerificationStatus();
        issuer.setCompanyName(request.companyName().trim());
        issuer.setRegistrationNumber(request.registrationNumber());
        issuer.setVerificationStatus(VerificationStatus.PENDING);
        issuerRepository.save(issuer);

        auditService.log(
                user,
                AuditActorRole.ISSUER,
                AuditActions.ISSUER_KYB_RESUBMITTED,
                AuditEntityTypes.ISSUER,
                issuer.getId(),
                Map.of("verificationStatus", before.name()),
                Map.of(
                        "verificationStatus", VerificationStatus.PENDING.name(),
                        "companyName", issuer.getCompanyName()
                )
        );

        return IssuerMapper.toResponse(issuer);
    }

    @Transactional(readOnly = true)
    public IssuerProfileResponse getProfile(UUID userPublicId) {
        Issuer issuer = issuerRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));
        return IssuerMapper.toResponse(issuer);
    }
}
