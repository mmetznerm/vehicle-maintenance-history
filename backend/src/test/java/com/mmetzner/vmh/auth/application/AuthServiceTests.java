package com.mmetzner.vmh.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mmetzner.vmh.auth.application.dto.AuthTokensResponse;
import com.mmetzner.vmh.auth.application.dto.LoginRequest;
import com.mmetzner.vmh.auth.application.dto.LogoutRequest;
import com.mmetzner.vmh.auth.application.dto.RefreshTokenRequest;
import com.mmetzner.vmh.auth.application.dto.RegisterRequest;
import com.mmetzner.vmh.auth.domain.model.RefreshToken;
import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.domain.repository.RefreshTokenRepository;
import com.mmetzner.vmh.auth.domain.repository.UserRepository;
import com.mmetzner.vmh.auth.infrastructure.security.JwtProperties;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.exception.ConflictException;
import com.mmetzner.vmh.shared.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final String JWT_SECRET =
            "test-secret-key-with-at-least-32-characters";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                JWT_SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );

        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                new JwtService(properties),
                properties
        );
    }

    @Test
    void registersUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest(
                "Maycon Metzner",
                "maycon@example.com",
                "password123"
        );

        when(userRepository.existsByEmailOrPhone(
                request.emailOrPhone()
        )).thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);

                    return new User(
                            UUID.randomUUID(),
                            user.fullName(),
                            user.emailOrPhone(),
                            user.passwordHash()
                    );
                });

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokensResponse response =
                authService.register(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.emailOrPhone())
                .isEqualTo("maycon@example.com");

        assertThat(passwordEncoder.matches(
                "password123",
                savedUser.passwordHash()
        )).isTrue();

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .save(tokenCaptor.capture());

        assertThat(tokenCaptor.getValue().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.refreshToken());
    }

    @Test
    void rejectsDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Maycon Metzner",
                "maycon@example.com",
                "password123"
        );

        when(userRepository.existsByEmailOrPhone(
                request.emailOrPhone()
        )).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage(ApiMessages.Users.ALREADY_REGISTERED)
                .extracting("code")
                .isEqualTo(ApiErrorCode.USER_ALREADY_REGISTERED);

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logsInWithValidCredentials() {
        User user = userWithPassword("password123");

        when(userRepository.findByEmailOrPhone(
                "maycon@example.com"
        )).thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokensResponse response = authService.login(
                new LoginRequest(
                        "maycon@example.com",
                        "password123"
                )
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void rejectsInvalidPassword() {
        User user = userWithPassword("password123");

        when(userRepository.findByEmailOrPhone(
                "maycon@example.com"
        )).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(
                        "maycon@example.com",
                        "wrong-password"
                )
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(ApiMessages.Auth.INVALID_CREDENTIALS);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotatesValidRefreshToken() {
        String rawToken = "existing-refresh-token";
        User user = userWithPassword("password123");

        RefreshToken storedToken = new RefreshToken(
                UUID.randomUUID(),
                user.id(),
                sha256(rawToken),
                Instant.now().plus(Duration.ofDays(1)),
                null
        );

        when(refreshTokenRepository.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(storedToken));

        when(userRepository.findById(user.id()))
                .thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokensResponse response = authService.refresh(
                new RefreshTokenRequest(rawToken)
        );

        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(response.refreshToken())
                .isNotBlank()
                .isNotEqualTo(rawToken);

        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void logoutRevokesExistingToken() {
        String rawToken = "existing-refresh-token";

        RefreshToken storedToken = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                sha256(rawToken),
                Instant.now().plus(Duration.ofDays(1)),
                null
        );

        when(refreshTokenRepository.findByTokenHash(
                sha256(rawToken)
        )).thenReturn(Optional.of(storedToken));

        authService.logout(new LogoutRequest(rawToken));

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    private User userWithPassword(String password) {
        return new User(
                UUID.randomUUID(),
                "Maycon Metzner",
                "maycon@example.com",
                passwordEncoder.encode(password)
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}