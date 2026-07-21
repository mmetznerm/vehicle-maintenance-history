package com.mmetzner.vmh.consistency.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.consistency.detection.DetectedInconsistency;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertOutboxWriter {

    private final AlertOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void detected(DetectedInconsistency alert, String correlationId) {
        write(
                "MaintenanceInconsistencyDetected",
                alert.alertId(), alert.vehicleId(), alert.rule().name(), alert.severity().name(),
                alert.maintenanceIds(), alert.summary(), alert.details(), correlationId
        );
    }

    public void resolved(ActiveInconsistencyEntity alert, String correlationId) {
        List<UUID> maintenanceIds = Arrays.stream(alert.getMaintenanceIds().split(","))
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .toList();
        write(
                "MaintenanceInconsistencyResolved",
                alert.getAlertId(), alert.getVehicleId(), alert.getRule(), alert.getSeverity(),
                maintenanceIds, alert.getSummary(),
                "The source records were corrected and no longer violate this rule.", correlationId
        );
    }

    private void write(
            String eventType,
            UUID alertId,
            UUID vehicleId,
            String rule,
            String severity,
            List<UUID> maintenanceIds,
            String summary,
            String details,
            String correlationId
    ) {
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        UUID eventId = UUID.randomUUID();
        JsonNode envelope = objectMapper.valueToTree(new AlertEnvelope(
                eventId, eventType, 1, alertId, vehicleId, occurredAt,
                correlationId == null || correlationId.isBlank() ? "consistency-worker" : correlationId,
                new AlertPayload(rule, severity, maintenanceIds, summary, details)
        ));
        repository.save(new AlertOutboxEntity(
                eventId,
                vehicleId,
                eventType,
                envelope,
                occurredAt
        ));
    }

    private record AlertEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID alertId,
            UUID vehicleId,
            OffsetDateTime occurredAt,
            String correlationId,
            AlertPayload payload
    ) {
    }

    private record AlertPayload(
            String rule,
            String severity,
            List<UUID> maintenanceIds,
            String summary,
            String details
    ) {
    }
}
