package com.mmetzner.vmh.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.shared.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthTokensResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailOrPhone(
                request.emailOrPhone()
        )) {
            log.info(
                    "User registration rejected reason=already_registered"
            );

            throw new ConflictException(
                    ApiErrorCode.USER_ALREADY_REGISTERED,
                    ApiMessages.Users.ALREADY_REGISTERED
            );
        }

        User user = new User(
                null,
                request.fullName(),
                request.emailOrPhone(),
                passwordEncoder.encode(request.password())
        );

        User savedUser = userRepository.save(user);

        log.info("User registered userId={}", savedUser.id());

        return createTokenResponse(savedUser);
    }

    @Transactional
    public AuthTokensResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailOrPhone(request.emailOrPhone())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(
                request.password(),
                user.passwordHash()
        )) {
            throw invalidCredentials();
        }

        log.info("User login succeeded userId={}", user.id());

        return createTokenResponse(user);
    }

    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashRefreshToken(
                request.refreshToken()
        );

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        Instant now = Instant.now();

        if (storedToken.isRevoked() || storedToken.isExpired(now)) {
            throw invalidRefreshToken();
        }

        storedToken.revoke(now);
        refreshTokenRepository.save(storedToken);

        User user = userRepository
                .findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.USER_NOT_FOUND,
                        ApiMessages.Users.NOT_FOUND
                ));

        log.info(
                "Refresh token rotated userId={}",
                storedToken.getUserId()
        );

        return createTokenResponse(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = hashRefreshToken(
                request.refreshToken()
        );

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> {
                    token.revoke(Instant.now());
                    refreshTokenRepository.save(token);

                    log.info(
                            "Refresh token revoked userId={}",
                            token.getUserId()
                    );
                });
    }

    private AuthTokensResponse createTokenResponse(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = createOpaqueRefreshToken();

        RefreshToken storedToken = new RefreshToken(
                null,
                user.id(),
                hashRefreshToken(refreshToken),
                Instant.now().plus(
                        jwtProperties.refreshTokenTtl()
                ),
                null
        );

        refreshTokenRepository.save(storedToken);

        return new AuthTokensResponse(
                accessToken,
                refreshToken
        );
    }

    private String createOpaqueRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(
                ApiErrorCode.INVALID_CREDENTIALS,
                ApiMessages.Auth.INVALID_CREDENTIALS
        );
    }

    private UnauthorizedException invalidRefreshToken() {
        return new UnauthorizedException(
                ApiErrorCode.INVALID_REFRESH_TOKEN,
                ApiMessages.Auth.INVALID_REFRESH_TOKEN
        );
    }
}