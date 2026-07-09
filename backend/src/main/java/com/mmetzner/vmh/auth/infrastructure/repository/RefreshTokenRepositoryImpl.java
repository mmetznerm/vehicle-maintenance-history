package com.mmetzner.vmh.auth.infrastructure.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.mmetzner.vmh.auth.domain.model.RefreshToken;
import com.mmetzner.vmh.auth.domain.repository.RefreshTokenRepository;
import com.mmetzner.vmh.auth.infrastructure.mapper.RefreshTokenEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl
        implements RefreshTokenRepository {

    private final RefreshTokenRepositoryJpa refreshTokenRepositoryJpa;
    private final RefreshTokenEntityMapper entityMapper;

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenRepositoryJpa
                .findByTokenHash(tokenHash)
                .map(entityMapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return entityMapper.toDomain(
                refreshTokenRepositoryJpa.save(entityMapper.toEntity(token))
        );
    }
}