package com.tranche.issuer.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.domain.Role;
import com.tranche.common.domain.VerificationStatus;
import com.tranche.common.exception.BusinessException;
import com.tranche.common.exception.ConflictException;
import com.tranche.common.exception.ForbiddenException;
import com.tranche.issuer.domain.Issuer;
import com.tranche.issuer.dto.CreateIssuerProfileRequest;
import com.tranche.issuer.repository.IssuerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssuerProfileServiceTest {

    @Mock
    private IssuerRepository issuerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.tranche.audit.service.AuditService auditService;

    @InjectMocks
    private IssuerProfileService issuerProfileService;

    @Test
    void createProfileForIssuer() {
        UUID publicId = UUID.randomUUID();
        User user = issuerUser(publicId);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(issuerRepository.existsByUser_Id(1L)).thenReturn(false);
        when(issuerRepository.save(any(Issuer.class))).thenAnswer(invocation -> {
            Issuer issuer = invocation.getArgument(0);
            issuer.getUser().setId(1L);
            return issuer;
        });

        var response = issuerProfileService.createProfile(
                publicId,
                new CreateIssuerProfileRequest("Acme Corp", "REG-12345")
        );

        assertThat(response.companyName()).isEqualTo("Acme Corp");
        assertThat(response.userId()).isEqualTo(publicId);
    }

    @Test
    void resubmitProfileAfterRejection() {
        UUID publicId = UUID.randomUUID();
        User user = issuerUser(publicId);
        Issuer issuer = new Issuer();
        issuer.setUser(user);
        issuer.setCompanyName("Old Corp");
        issuer.setVerificationStatus(VerificationStatus.REJECTED);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(issuerRepository.findByUser_PublicId(publicId)).thenReturn(Optional.of(issuer));
        when(issuerRepository.save(any(Issuer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = issuerProfileService.resubmitProfile(
                publicId,
                new CreateIssuerProfileRequest("New Corp", "REG-999")
        );

        assertThat(response.companyName()).isEqualTo("New Corp");
        assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
        verify(auditService).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsNonIssuerUser() {
        UUID publicId = UUID.randomUUID();
        User user = new User();
        user.setId(2L);
        user.setPublicId(publicId);
        user.setRole(Role.INVESTOR);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> issuerProfileService.createProfile(
                publicId,
                new CreateIssuerProfileRequest("Acme Corp", null)
        )).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsDuplicateProfile() {
        UUID publicId = UUID.randomUUID();
        User user = issuerUser(publicId);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(issuerRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> issuerProfileService.createProfile(
                publicId,
                new CreateIssuerProfileRequest("Acme Corp", null)
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    void resubmitRejectsWhenNotRejected() {
        UUID publicId = UUID.randomUUID();
        User user = issuerUser(publicId);
        Issuer issuer = new Issuer();
        issuer.setUser(user);
        issuer.setVerificationStatus(VerificationStatus.PENDING);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));
        when(issuerRepository.findByUser_PublicId(publicId)).thenReturn(Optional.of(issuer));

        assertThatThrownBy(() -> issuerProfileService.resubmitProfile(
                publicId,
                new CreateIssuerProfileRequest("Acme Corp", null)
        )).isInstanceOf(BusinessException.class);
    }

    private User issuerUser(UUID publicId) {
        User user = new User();
        user.setId(1L);
        user.setPublicId(publicId);
        user.setRole(Role.ISSUER);
        return user;
    }
}
