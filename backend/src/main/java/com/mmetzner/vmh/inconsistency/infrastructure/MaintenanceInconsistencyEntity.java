package com.mmetzner.vmh.inconsistency.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "maintenance_inconsistencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaintenanceInconsistencyEntity {

    @Id
    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String rule;

    @Column(nullable = false, length = 20)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "maintenance_ids", nullable = false, columnDefinition = "jsonb")
    private JsonNode maintenanceIds;

    @Column(nullable = false, length = 200)
    private String summary;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "last_event_at", nullable = false)
    private OffsetDateTime lastEventAt;

    public MaintenanceInconsistencyEntity(UUID alertId, UUID vehicleId) {
        this.alertId = alertId;
        this.vehicleId = vehicleId;
    }

    public void detect(
            String rule,
            String severity,
            JsonNode maintenanceIds,
            String summary,
            String details,
            OffsetDateTime occurredAt
    ) {
        this.rule = rule;
        this.severity = severity;
        this.maintenanceIds = maintenanceIds;
        this.summary = summary;
        this.details = details;
        this.status = "ACTIVE";
        if (detectedAt == null) {
            this.detectedAt = occurredAt;
        }
        this.resolvedAt = null;
        this.lastEventAt = occurredAt;
    }

    public void resolve(OffsetDateTime occurredAt) {
        this.status = "RESOLVED";
        this.resolvedAt = occurredAt;
        this.lastEventAt = occurredAt;
    }
}
