package com.mmetzner.vmh.shared.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.mmetzner.vmh.shared.infrastructure.web.RequestIdFilter.REQUEST_ID_MDC_KEY;

@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public UUID write(EventType type, UUID aggregateId, UUID vehicleId, Object eventPayload) {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        String correlationId = MDC.get(REQUEST_ID_MDC_KEY);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", type.externalName());
        envelope.put("eventVersion", 1);
        envelope.put("aggregateId", aggregateId.toString());
        envelope.put("vehicleId", vehicleId.toString());
        envelope.put("occurredAt", occurredAt.toString());
        envelope.put("correlationId", correlationId);
        JsonNode payload = eventPayload == null
                ? objectMapper.createObjectNode()
                : objectMapper.valueToTree(eventPayload);
        envelope.set("payload", payload);

        repository.save(OutboxEventEntity.create(
                eventId,
                type,
                aggregateId,
                vehicleId,
                envelope,
                occurredAt
        ));

        return eventId;
    }
}
