package com.mmetzner.vmh.auth.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.mmetzner.vmh.auth.domain.model.User;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmailOrPhone(String emailOrPhone);

    boolean existsByEmailOrPhone(String emailOrPhone);

    User save(User user);
}