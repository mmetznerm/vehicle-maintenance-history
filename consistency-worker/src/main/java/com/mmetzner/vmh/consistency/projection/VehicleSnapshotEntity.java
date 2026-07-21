package com.mmetzner.vmh.consistency.projection;

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
@Table(name = "vehicle_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleSnapshotEntity {

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public VehicleSnapshotEntity(UUID vehicleId, OffsetDateTime updatedAt) {
        this.vehicleId = vehicleId;
        this.updatedAt = updatedAt;
    }

    public void update(Integer manufactureYear, OffsetDateTime updatedAt) {
        this.manufactureYear = manufactureYear;
        this.updatedAt = updatedAt;
    }
}
