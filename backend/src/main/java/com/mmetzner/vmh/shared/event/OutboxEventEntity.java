package com.mmetzner.vmh.shared.event;

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
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Long sequenceId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

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

    private OutboxEventEntity(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            UUID vehicleId,
            String eventType,
            Integer eventVersion,
            JsonNode payload,
            OffsetDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.vehicleId = vehicleId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.attempts = 0;
    }

    public static OutboxEventEntity create(
            UUID eventId,
            EventType eventType,
            UUID aggregateId,
            UUID vehicleId,
            JsonNode payload,
            OffsetDateTime occurredAt
    ) {
        return new OutboxEventEntity(
                eventId,
                eventType.aggregateType(),
                aggregateId,
                vehicleId,
                eventType.externalName(),
                1,
                payload,
                occurredAt
        );
    }

    public void markPublished(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error == null ? "Unknown Kafka publication error" : error.substring(0, Math.min(error.length(), 1000));
    }
}
