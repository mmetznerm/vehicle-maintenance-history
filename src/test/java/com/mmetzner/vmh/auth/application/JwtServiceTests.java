package com.mmetzner.vmh.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.infrastructure.security.JwtProperties;

class JwtServiceTests {

    private static final String TEST_SECRET =
            "test-secret-key-with-at-least-32-characters";

    @Test
    void createsTokenContainingUserId() {
        JwtProperties properties = new JwtProperties(
                TEST_SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(30)
        );

        JwtService jwtService = new JwtService(properties);

        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "Maycon Metzner",
                "maycon@example.com",
                "stored-password-hash"
        );

        String token = jwtService.createAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }
}