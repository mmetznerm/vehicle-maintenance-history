package com.mmetzner.vmh.history.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "maintenance_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaintenanceHistoryEntity {

    @Id
    @Column(name = "maintenance_id")
    private UUID maintenanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleHistoryEntity vehicle;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(nullable = false)
    private Integer odometer;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MaintenanceHistoryEntity(UUID maintenanceId, VehicleHistoryEntity vehicle) {
        this.maintenanceId = maintenanceId;
        this.vehicle = vehicle;
    }

    public void update(
            LocalDate maintenanceDate,
            Integer odometer,
            String description,
            BigDecimal cost,
            OffsetDateTime updatedAt
    ) {
        this.maintenanceDate = maintenanceDate;
        this.odometer = odometer;
        this.description = description;
        this.cost = cost;
        this.updatedAt = updatedAt;
    }
}
