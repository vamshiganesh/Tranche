package com.tranche.auth.service;

import com.tranche.auth.domain.User;
import com.tranche.auth.repository.UserRepository;
import com.tranche.common.config.JwtProperties;
import com.tranche.common.domain.Role;
import com.tranche.common.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                new JwtProperties("test-secret-key-with-minimum-32-characters", 3_600_000L),
                userRepository
        );
    }

    @Test
    void generatesAndParsesToken() {
        UUID publicId = UUID.randomUUID();
        User user = activeUser(publicId, "investor@example.com", Role.INVESTOR);
        UserPrincipal principal = UserPrincipal.from(user);

        when(userRepository.findByPublicId(publicId)).thenReturn(Optional.of(user));

        String token = jwtService.generateToken(principal);
        Optional<UserPrincipal> parsed = jwtService.parseToken(token);

        assertThat(token).isNotBlank();
        assertThat(parsed).isPresent();
        assertThat(parsed.get().getPublicId()).isEqualTo(publicId);
        assertThat(parsed.get().getRole()).isEqualTo(Role.INVESTOR);
    }

    @Test
    void rejectsInvalidToken() {
        assertThat(jwtService.parseToken("not-a-valid-token")).isEmpty();
    }

    private User activeUser(UUID publicId, String email, Role role) {
        User user = new User();
        user.setId(1L);
        user.setPublicId(publicId);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFullName("Test User");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
