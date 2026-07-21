package com.mmetzner.vmh.vehicle.infrastructure.entity;

import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "vehicles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 10)
    private String plate;

    @Column(nullable = false, length = 80)
    private String brand;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private Integer manufactureYear;

    @Column(length = 40)
    private String color;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "history_sharing_enabled", nullable = false)
    private boolean historySharingEnabled;

    @Column(name = "history_public_id", unique = true)
    private UUID historyPublicId;

    public VehicleEntity(
            UUID id,
            UserEntity owner,
            String plate,
            String brand,
            String model,
            Integer manufactureYear,
            String color,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            boolean historySharingEnabled,
            UUID historyPublicId
    ) {
        this.id = id;
        this.owner = owner;
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.manufactureYear = manufactureYear;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.historySharingEnabled = historySharingEnabled;
        this.historyPublicId = historyPublicId;
    }

    public static VehicleEntity reference(UUID id) {
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.id = id;
        return vehicle;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
