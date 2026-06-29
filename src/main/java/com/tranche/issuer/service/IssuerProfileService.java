package com.tranche.issuer.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.exception.ConflictException;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.dto.CreateIssuerProfileRequest;
import com.tranche.issuer.dto.IssuerProfileResponse;
import com.tranche.issuer.repository.IssuerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IssuerProfileService {

    private final IssuerRepository issuerRepository;
    private final UserRepository userRepository;

    public IssuerProfileService(IssuerRepository issuerRepository, UserRepository userRepository) {
        this.issuerRepository = issuerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public IssuerProfileResponse createProfile(UUID userPublicId, CreateIssuerProfileRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.ISSUER) {
            throw new ForbiddenException("Only issuer accounts can create an issuer profile");
        }
        if (issuerRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Issuer profile already exists");
        }

        Issuer issuer = new Issuer();
        issuer.setUser(user);
        issuer.setCompanyName(request.companyName().trim());
        issuer.setRegistrationNumber(request.registrationNumber());
        issuerRepository.save(issuer);

        return toResponse(issuer);
    }

    @Transactional(readOnly = true)
    public IssuerProfileResponse getProfile(UUID userPublicId) {
        Issuer issuer = issuerRepository.findByUser_PublicId(userPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));
        return toResponse(issuer);
    }

    private IssuerProfileResponse toResponse(Issuer issuer) {
        return new IssuerProfileResponse(
                issuer.getId(),
                issuer.getCompanyName(),
                issuer.getRegistrationNumber(),
                issuer.getUser().getPublicId()
        );
    }
}
