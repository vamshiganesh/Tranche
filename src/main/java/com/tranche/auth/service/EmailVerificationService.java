package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.config.OnboardingProperties;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ErrorCode;
import com.tranche.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int CODE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final OnboardingProperties onboardingProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(UserRepository userRepository, OnboardingProperties onboardingProperties) {
        this.userRepository = userRepository;
        this.onboardingProperties = onboardingProperties;
    }

    @Transactional
    public String issueVerificationCode(User user) {
        String code = generateCode();
        user.setEmailVerificationCode(code);
        user.setEmailVerificationExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        user.setEmailVerified(false);
        userRepository.save(user);

        log.info("Email verification code for {}: {}", user.getEmail(), code);
        return onboardingProperties.exposeVerificationCode() ? code : null;
    }

    @Transactional
    public void verify(String email, String code) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getEmailVerificationCode() == null
                || user.getEmailVerificationExpiresAt() == null
                || Instant.now().isAfter(user.getEmailVerificationExpiresAt())) {
            throw new BusinessException(
                    ErrorCode.INVALID_VERIFICATION_CODE,
                    "Verification code has expired. Request a new code."
            );
        }

        if (!user.getEmailVerificationCode().equals(code.trim())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public String resend(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            return null;
        }
        return issueVerificationCode(user);
    }

    private String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
