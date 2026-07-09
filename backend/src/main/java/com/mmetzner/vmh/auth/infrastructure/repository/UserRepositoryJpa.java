package com.mmetzner.vmh.auth.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;

interface UserRepositoryJpa extends JpaRepository<UserEntity, UUID> {

    @Query("""
            select user
            from UserEntity user
            where user.emailOrPhone = :emailOrPhone
            """)
    Optional<UserEntity> findByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    @Query("""
            select count(user) > 0
            from UserEntity user
            where user.emailOrPhone = :emailOrPhone
            """)
    boolean existsByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);
}