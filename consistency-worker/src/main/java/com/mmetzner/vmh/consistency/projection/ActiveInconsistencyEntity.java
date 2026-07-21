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
@Table(name = "active_inconsistencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveInconsistencyEntity {

    @Id
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String rule;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "maintenance_ids", nullable = false, columnDefinition = "text")
    private String maintenanceIds;

    @Column(nullable = false, length = 200)
    private String summary;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    public ActiveInconsistencyEntity(
            UUID alertId,
            UUID vehicleId,
            String rule,
            String severity,
            String maintenanceIds,
            String summary,
            String details,
            OffsetDateTime detectedAt
    ) {
        this.alertId = alertId;
        this.vehicleId = vehicleId;
        this.rule = rule;
        this.severity = severity;
        this.maintenanceIds = maintenanceIds;
        this.summary = summary;
        this.details = details;
        this.detectedAt = detectedAt;
    }
}
