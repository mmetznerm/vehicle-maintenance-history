package com.mmetzner.vmh.auth.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Repository;

import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.domain.repository.UserRepository;
import com.mmetzner.vmh.auth.infrastructure.mapper.UserEntityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserRepositoryJpa springDataRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public Optional<User> findById(UUID id) {
        return springDataRepository
                .findById(id)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailOrPhone(String emailOrPhone) {
        return springDataRepository
                .findByEmailOrPhone(emailOrPhone)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmailOrPhone(String emailOrPhone) {
        return springDataRepository
                .existsByEmailOrPhone(emailOrPhone);
    }

    @Override
    public User save(User user) {
        UserEntity savedEntity = springDataRepository.save(
                userEntityMapper.toEntity(user)
        );

        return userEntityMapper.toDomain(savedEntity);
    }
}