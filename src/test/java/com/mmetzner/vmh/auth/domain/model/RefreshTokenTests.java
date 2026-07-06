package com.mmetzner.vmh.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokenTests {

    @Test
    void revokesActiveToken() {
        Instant now = Instant.now();

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "token-hash",
                now.plusSeconds(60),
                null
        );

        assertThat(token.isRevoked()).isFalse();

        token.revoke(now);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void considersTokenExpiredAtExpirationInstant() {
        Instant expiresAt = Instant.now();

        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "token-hash",
                expiresAt,
                null
        );

        assertThat(token.isExpired(expiresAt)).isTrue();
    }
}