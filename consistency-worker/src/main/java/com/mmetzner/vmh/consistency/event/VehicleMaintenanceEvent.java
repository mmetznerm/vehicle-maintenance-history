package com.mmetzner.vmh.consistency.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleMaintenanceEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        UUID vehicleId,
        OffsetDateTime occurredAt,
        String correlationId,
        JsonNode payload
) {
}
