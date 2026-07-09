package com.mmetzner.vmh.auth.infrastructure.mapper;

import org.springframework.stereotype.Component;

import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;

@Component
public class UserEntityMapper {

    public User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getEmailOrPhone(),
                entity.getPasswordHash()
        );
    }

    public UserEntity toEntity(User user) {
        if (user.id() == null) {
            return new UserEntity(
                    user.fullName(),
                    user.emailOrPhone(),
                    user.passwordHash()
            );
        }

        return new UserEntity(
                user.id(),
                user.fullName(),
                user.emailOrPhone(),
                user.passwordHash()
        );
    }
}