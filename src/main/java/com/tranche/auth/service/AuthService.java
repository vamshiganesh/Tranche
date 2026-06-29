package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.dto.CurrentUserResponse;
import com.tranche.auth.dto.LoginRequest;
import com.tranche.auth.dto.LoginResponse;
import com.tranche.auth.dto.RegisterRequest;
import com.tranche.auth.dto.UserResponse;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ConflictException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.common.exception.ResourceNotFoundException;
import com.tranche.common.security.UserPrincipal;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.investor.service.InvestorProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InvestorProfileService investorProfileService;
    private final InvestorProfileRepository investorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            InvestorProfileService investorProfileService,
            InvestorProfileRepository investorProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.investorProfileService = investorProfileService;
        this.investorProfileRepository = investorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenException("Admin accounts cannot be registered via API");
        }
        if (request.role() != Role.INVESTOR && request.role() != Role.ISSUER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE, "Invalid registration role");
        }
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setPublicId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        userRepository.save(user);

        if (request.role() == Role.INVESTOR) {
            investorProfileService.createForUser(user);
        }

        return UserMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserMapper.toUserResponse(user)
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return investorProfileRepository.findByUserId(user.getId())
                .map(profile -> UserMapper.toCurrentUserResponse(user, profile))
                .orElseGet(() -> UserMapper.toCurrentUserResponse(user, null));
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
