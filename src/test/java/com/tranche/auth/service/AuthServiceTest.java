package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.dto.LoginRequest;
import com.tranche.auth.dto.RegisterRequest;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ConflictException;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.investor.domain.InvestorProfile;
import com.tranche.investor.repository.InvestorProfileRepository;
import com.tranche.investor.service.InvestorProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InvestorProfileService investorProfileService;
    @Mock
    private InvestorProfileRepository investorProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerInvestorCreatesProfile() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.existsByEmail("investor@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        var response = authService.register(new RegisterRequest(
                "investor@example.com",
                "Password123!",
                Role.INVESTOR,
                "Jane Investor"
        ));

        assertThat(response.email()).isEqualTo("investor@example.com");
        assertThat(response.role()).isEqualTo(Role.INVESTOR);
        verify(investorProfileService).createForUser(any(User.class));
    }

    @Test
    void registerIssuerDoesNotCreateInvestorProfile() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.existsByEmail("issuer@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest(
                "issuer@example.com",
                "Password123!",
                Role.ISSUER,
                "Acme Issuer"
        ));

        verify(investorProfileService, never()).createForUser(any(User.class));
    }

    @Test
    void registerRejectsAdminRole() {
        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "admin@example.com",
                "Password123!",
                Role.ADMIN,
                "Admin"
        ))).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("investor@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest(
                "investor@example.com",
                "Password123!",
                Role.INVESTOR,
                "Jane Investor"
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void loginReturnsJwtForValidCredentials() {
        User user = new User();
        user.setId(1L);
        user.setPublicId(java.util.UUID.randomUUID());
        user.setEmail("investor@example.com");
        user.setPasswordHash("encoded-password");
        user.setFullName("Jane Investor");
        user.setRole(Role.INVESTOR);
        user.setEnabled(true);

        when(userRepository.findByEmail("investor@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = authService.login(new LoginRequest("investor@example.com", "Password123!"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("investor@example.com");
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = new User();
        user.setEmail("investor@example.com");
        user.setPasswordHash("encoded-password");
        user.setEnabled(true);

        when(userRepository.findByEmail("investor@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("investor@example.com", "wrong-password")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getCurrentUserIncludesWalletBalanceForInvestor() {
        java.util.UUID publicId = java.util.UUID.randomUUID();
        User user = new User();
        user.setId(1L);
        user.setPublicId(publicId);
        user.setEmail("investor@example.com");
        user.setFullName("Jane Investor");
        user.setRole(Role.INVESTOR);

        InvestorProfile profile = new InvestorProfile();
        profile.setWalletBalance(new BigDecimal("500000.0000"));

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(investorProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));

        var response = authService.getCurrentUser(publicId);

        assertThat(response.walletBalance()).isEqualByComparingTo("500000.0000");
    }
}
