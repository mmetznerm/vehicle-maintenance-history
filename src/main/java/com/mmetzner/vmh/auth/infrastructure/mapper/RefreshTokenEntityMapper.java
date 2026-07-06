package com.mmetzner.vmh.auth.infrastructure.mapper;

import org.springframework.stereotype.Component;

import com.mmetzner.vmh.auth.domain.model.RefreshToken;
import com.mmetzner.vmh.auth.infrastructure.entity.RefreshTokenEntity;
import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;

@Component
public class RefreshTokenEntityMapper {

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getRevokedAt()
        );
    }

    public RefreshTokenEntity toEntity(RefreshToken token) {
        return new RefreshTokenEntity(
                token.getId(),
                UserEntity.reference(token.getUserId()),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.getRevokedAt()
        );
    }
}