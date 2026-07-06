package com.mmetzner.vmh.auth.domain.repository;

import java.util.Optional;

import com.mmetzner.vmh.auth.domain.model.RefreshToken;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);
}