package com.mmetzner.vmh.inconsistency.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MaintenanceInconsistencyAlertEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID alertId,
        UUID vehicleId,
        OffsetDateTime occurredAt,
        String correlationId,
        JsonNode payload
) {
}
