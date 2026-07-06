package com.mmetzner.vmh.auth.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mmetzner.vmh.auth.infrastructure.entity.RefreshTokenEntity;

interface RefreshTokenRepositoryJpa
        extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}