package com.mmetzner.vmh.history.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.history.projection.MaintenanceHistoryEntity;
import com.mmetzner.vmh.history.projection.MaintenanceHistoryRepository;
import com.mmetzner.vmh.history.projection.ProcessedEventEntity;
import com.mmetzner.vmh.history.projection.ProcessedEventId;
import com.mmetzner.vmh.history.projection.ProcessedEventRepository;
import com.mmetzner.vmh.history.projection.VehicleHistoryEntity;
import com.mmetzner.vmh.history.projection.VehicleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleHistoryEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(VehicleHistoryEventProcessor.class);

    private final ObjectMapper objectMapper;
    private final VehicleHistoryRepository vehicleRepository;
    private final MaintenanceHistoryRepository maintenanceRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${app.kafka.consumer-group}")
    private String consumerName;

    @Transactional
    public void process(String eventJson) {
        VehicleMaintenanceEvent event = readEvent(eventJson);
        ProcessedEventId processedEventId = new ProcessedEventId(event.eventId(), consumerName);

        if (processedEventRepository.existsById(processedEventId)) {
            log.info("Duplicate event ignored eventId={} eventType={} vehicleId={} consumerGroup={}",
                    event.eventId(), event.eventType(), event.vehicleId(), consumerName);
            return;
        }

        if (event.eventVersion() == 1) {
            apply(event);
        } else {
            log.warn("Unsupported event version ignored eventId={} eventType={} eventVersion={}",
                    event.eventId(), event.eventType(), event.eventVersion());
        }

        processedEventRepository.save(new ProcessedEventEntity(
                event.eventId(),
                consumerName,
                OffsetDateTime.now(ZoneOffset.UTC)
        ));

        log.info("Event processed eventId={} eventType={} vehicleId={} consumerGroup={} correlationId={}",
                event.eventId(), event.eventType(), event.vehicleId(), consumerName, event.correlationId());
    }

    private VehicleMaintenanceEvent readEvent(String eventJson) {
        try {
            VehicleMaintenanceEvent event = objectMapper.readValue(eventJson, VehicleMaintenanceEvent.class);
            if (event.eventId() == null || event.eventType() == null || event.vehicleId() == null
                    || event.occurredAt() == null || event.payload() == null) {
                throw new IllegalArgumentException("Event envelope contains missing required fields");
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid vehicle maintenance event", exception);
        }
    }

    private void apply(VehicleMaintenanceEvent event) {
        switch (event.eventType()) {
            case "VehicleCreated", "VehicleUpdated" -> upsertVehicle(event);
            case "VehicleDeleted" -> vehicleRepository.deleteById(event.vehicleId());
            case "VehicleHistorySharingChanged" -> updateSharing(event);
            case "MaintenanceCreated", "MaintenanceUpdated" -> upsertMaintenance(event);
            case "MaintenanceDeleted" -> maintenanceRepository.deleteById(event.aggregateId());
            default -> log.info("Unknown event type ignored eventId={} eventType={}",
                    event.eventId(), event.eventType());
        }
    }

    private void upsertVehicle(VehicleMaintenanceEvent event) {
        JsonNode payload = event.payload();
        VehicleHistoryEntity vehicle = vehicleRepository.findById(event.vehicleId())
                .orElseGet(() -> new VehicleHistoryEntity(event.vehicleId()));
        vehicle.updateDetails(
                requiredText(payload, "brand"),
                requiredText(payload, "model"),
                requiredInteger(payload, "manufactureYear"),
                nullableText(payload, "color"),
                event.occurredAt()
        );
        vehicleRepository.save(vehicle);
    }

    private void updateSharing(VehicleMaintenanceEvent event) {
        JsonNode payload = event.payload();
        VehicleHistoryEntity vehicle = vehicleRepository.findById(event.vehicleId())
                .orElseGet(() -> new VehicleHistoryEntity(event.vehicleId()));
        boolean enabled = payload.path("enabled").asBoolean(false);
        UUID publicId = enabled ? UUID.fromString(requiredText(payload, "publicId")) : null;
        vehicle.updateSharing(enabled, publicId, event.occurredAt());
        vehicleRepository.save(vehicle);
    }

    private void upsertMaintenance(VehicleMaintenanceEvent event) {
        JsonNode payload = event.payload();
        VehicleHistoryEntity vehicle = vehicleRepository.findById(event.vehicleId())
                .orElseGet(() -> vehicleRepository.save(new VehicleHistoryEntity(event.vehicleId())));
        MaintenanceHistoryEntity maintenance = maintenanceRepository.findById(event.aggregateId())
                .orElseGet(() -> new MaintenanceHistoryEntity(event.aggregateId(), vehicle));
        maintenance.update(
                LocalDate.parse(requiredText(payload, "maintenanceDate")),
                requiredInteger(payload, "odometer"),
                requiredText(payload, "description"),
                requiredDecimal(payload, "cost"),
                event.occurredAt()
        );
        maintenanceRepository.save(maintenance);
    }

    private String requiredText(JsonNode payload, String field) {
        String value = nullableText(payload, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing event payload field: " + field);
        }
        return value;
    }

    private String nullableText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Integer requiredInteger(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new IllegalArgumentException("Missing event payload field: " + field);
        }
        return value.intValue();
    }

    private BigDecimal requiredDecimal(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("Missing event payload field: " + field);
        }
        return value.decimalValue();
    }
}
