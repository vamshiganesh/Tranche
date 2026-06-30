package com.tranche.auth.controller;

import com.tranche.auth.dto.CurrentUserResponse;
import com.tranche.auth.dto.LoginRequest;
import com.tranche.auth.dto.LoginResponse;
import com.tranche.auth.dto.RegisterRequest;
import com.tranche.auth.dto.RegisterResponse;
import com.tranche.auth.dto.ResendVerificationRequest;
import com.tranche.auth.dto.VerifyEmailRequest;
import com.tranche.auth.service.AuthService;
import com.tranche.auth.service.EmailVerificationService;
import com.tranche.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.email(), request.code());
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        String devCode = emailVerificationService.resend(request.email());
        if (devCode != null) {
            return Map.of("devVerificationCode", devCode);
        }
        return Map.of("message", "If the account exists and is unverified, a new code was sent.");
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return authService.getCurrentUser(SecurityUtils.requireCurrentPublicId());
    }
}
