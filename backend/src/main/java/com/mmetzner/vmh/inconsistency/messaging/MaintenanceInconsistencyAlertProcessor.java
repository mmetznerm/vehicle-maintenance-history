package com.mmetzner.vmh.inconsistency.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyEntity;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyRepository;
import com.mmetzner.vmh.inconsistency.infrastructure.ProcessedAlertEventEntity;
import com.mmetzner.vmh.inconsistency.infrastructure.ProcessedAlertEventId;
import com.mmetzner.vmh.inconsistency.infrastructure.ProcessedAlertEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class MaintenanceInconsistencyAlertProcessor {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceInconsistencyAlertProcessor.class);

    private final ObjectMapper objectMapper;
    private final MaintenanceInconsistencyRepository inconsistencyRepository;
    private final ProcessedAlertEventRepository processedEventRepository;

    @Value("${app.kafka.alert-topic.consumer-group}")
    private String consumerName;

    @Transactional
    public void process(String eventJson) {
        MaintenanceInconsistencyAlertEvent event = readEvent(eventJson);
        ProcessedAlertEventId id = new ProcessedAlertEventId(event.eventId(), consumerName);
        if (processedEventRepository.existsById(id)) {
            log.info("Duplicate alert event ignored eventId={} alertId={}", event.eventId(), event.alertId());
            return;
        }

        if (event.eventVersion() == 1) {
            switch (event.eventType()) {
                case "MaintenanceInconsistencyDetected" -> detect(event);
                case "MaintenanceInconsistencyResolved" -> resolve(event);
                default -> log.info("Unknown alert event ignored eventType={}", event.eventType());
            }
        } else {
            log.warn("Unsupported alert event version ignored eventId={} version={}",
                    event.eventId(), event.eventVersion());
        }

        processedEventRepository.save(new ProcessedAlertEventEntity(
                event.eventId(), consumerName, OffsetDateTime.now(ZoneOffset.UTC)
        ));
    }

    private void detect(MaintenanceInconsistencyAlertEvent event) {
        JsonNode payload = event.payload();
        MaintenanceInconsistencyEntity entity = inconsistencyRepository.findById(event.alertId())
                .orElseGet(() -> new MaintenanceInconsistencyEntity(event.alertId(), event.vehicleId()));
        if (entity.getLastEventAt() != null && entity.getLastEventAt().isAfter(event.occurredAt())) {
            return;
        }
        entity.detect(
                requiredText(payload, "rule"),
                requiredText(payload, "severity"),
                requiredArray(payload, "maintenanceIds"),
                requiredText(payload, "summary"),
                requiredText(payload, "details"),
                event.occurredAt()
        );
        inconsistencyRepository.save(entity);
    }

    private void resolve(MaintenanceInconsistencyAlertEvent event) {
        inconsistencyRepository.findById(event.alertId()).ifPresent(entity -> {
            if (entity.getLastEventAt() == null || !entity.getLastEventAt().isAfter(event.occurredAt())) {
                entity.resolve(event.occurredAt());
            }
        });
    }

    private MaintenanceInconsistencyAlertEvent readEvent(String json) {
        try {
            MaintenanceInconsistencyAlertEvent event = objectMapper.readValue(
                    json, MaintenanceInconsistencyAlertEvent.class
            );
            if (event.eventId() == null || event.eventType() == null || event.alertId() == null
                    || event.vehicleId() == null || event.occurredAt() == null || event.payload() == null) {
                throw new IllegalArgumentException("Alert event contains missing required fields");
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid maintenance inconsistency alert event", exception);
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing alert payload field: " + field);
        }
        return value.asText();
    }

    private JsonNode requiredArray(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new IllegalArgumentException("Missing alert payload field: " + field);
        }
        return value;
    }
}
