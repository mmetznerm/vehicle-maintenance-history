package com.mmetzner.vmh.history.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "vehicle_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleHistoryEntity {

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(nullable = false, length = 80)
    private String brand;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private Integer manufactureYear;

    @Column(length = 40)
    private String color;

    @Column(name = "sharing_enabled", nullable = false)
    private boolean sharingEnabled;

    @Column(name = "public_id", unique = true)
    private UUID publicId;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public VehicleHistoryEntity(UUID vehicleId) {
        this.vehicleId = vehicleId;
        this.brand = "Unknown";
        this.model = "Unknown";
        this.manufactureYear = 1886;
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateDetails(
            String brand,
            String model,
            Integer manufactureYear,
            String color,
            OffsetDateTime updatedAt
    ) {
        this.brand = brand;
        this.model = model;
        this.manufactureYear = manufactureYear;
        this.color = color;
        this.updatedAt = updatedAt;
    }

    public void updateSharing(boolean enabled, UUID publicId, OffsetDateTime updatedAt) {
        this.sharingEnabled = enabled;
        this.publicId = enabled ? publicId : null;
        this.updatedAt = updatedAt;
    }
}
