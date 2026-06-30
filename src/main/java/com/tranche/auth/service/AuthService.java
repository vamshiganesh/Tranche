package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.dto.CurrentUserResponse;
import com.tranche.auth.dto.LoginRequest;
import com.tranche.auth.dto.LoginResponse;
import com.tranche.auth.dto.RegisterRequest;
import com.tranche.auth.dto.RegisterResponse;
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
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.repository.IssuerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InvestorProfileService investorProfileService;
    private final InvestorProfileRepository investorProfileRepository;
    private final IssuerRepository issuerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            InvestorProfileService investorProfileService,
            InvestorProfileRepository investorProfileRepository,
            IssuerRepository issuerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.investorProfileService = investorProfileService;
        this.investorProfileRepository = investorProfileRepository;
        this.issuerRepository = issuerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
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
        user.setEmailVerified(false);
        userRepository.save(user);

        if (request.role() == Role.INVESTOR) {
            investorProfileService.createForUser(user);
        }

        String devCode = emailVerificationService.issueVerificationCode(user);

        return new RegisterResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.getCreatedAt(),
                true,
                devCode
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!user.isEmailVerified()) {
            throw new BusinessException(
                    ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email address is not verified. Check your inbox or request a new code."
            );
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

        InvestorProfile investorProfile = investorProfileRepository.findByUser_Id(user.getId()).orElse(null);
        Issuer issuer = issuerRepository.findByUser_Id(user.getId()).orElse(null);

        return UserMapper.toCurrentUserResponse(user, investorProfile, issuer);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
