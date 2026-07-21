package com.mmetzner.vmh.consistency.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "maintenance_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaintenanceSnapshotEntity {

    @Id
    @Column(name = "maintenance_id")
    private UUID maintenanceId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(nullable = false)
    private Integer odometer;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MaintenanceSnapshotEntity(UUID maintenanceId, UUID vehicleId) {
        this.maintenanceId = maintenanceId;
        this.vehicleId = vehicleId;
    }

    public void update(LocalDate maintenanceDate, Integer odometer, String description, OffsetDateTime updatedAt) {
        this.maintenanceDate = maintenanceDate;
        this.odometer = odometer;
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
