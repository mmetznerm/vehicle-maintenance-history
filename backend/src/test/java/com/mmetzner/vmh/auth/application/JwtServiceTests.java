package com.mmetzner.vmh.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.infrastructure.security.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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
        SecretKey signingKey = Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8)
        );
        String fullName = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("fullName", String.class);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(fullName).isEqualTo("Maycon Metzner");
    }
}
