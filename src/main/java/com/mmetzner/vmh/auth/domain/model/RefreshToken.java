package com.mmetzner.vmh.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

@Getter
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}