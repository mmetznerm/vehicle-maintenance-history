package com.mmetzner.vmh.consistency.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "alert_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Long sequenceId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public AlertOutboxEntity(
            UUID eventId,
            UUID vehicleId,
            String eventType,
            JsonNode payload,
            OffsetDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.vehicleId = vehicleId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.attempts = 0;
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        String message = error == null ? "Unknown Kafka publication error" : error;
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }
}
