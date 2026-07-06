package com.mmetzner.vmh.auth.infrastructure.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(
            name = "email_or_phone",
            nullable = false,
            unique = true,
            length = 180
    )
    private String emailOrPhone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserEntity(
            String fullName,
            String emailOrPhone,
            String passwordHash
    ) {
        this.fullName = fullName;
        this.emailOrPhone = emailOrPhone;
        this.passwordHash = passwordHash;
    }

    public UserEntity(
            UUID id,
            String fullName,
            String emailOrPhone,
            String passwordHash
    ) {
        this.id = id;
        this.fullName = fullName;
        this.emailOrPhone = emailOrPhone;
        this.passwordHash = passwordHash;
    }

    public static UserEntity reference(UUID id) {
        UserEntity user = new UserEntity();
        user.id = id;
        return user;
    }
}