package com.mmetzner.vmh.consistency.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.consistency.detection.InconsistencyReconciler;
import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotEntity;
import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotRepository;
import com.mmetzner.vmh.consistency.projection.ProcessedEventEntity;
import com.mmetzner.vmh.consistency.projection.ProcessedEventId;
import com.mmetzner.vmh.consistency.projection.ProcessedEventRepository;
import com.mmetzner.vmh.consistency.projection.VehicleSnapshotEntity;
import com.mmetzner.vmh.consistency.projection.VehicleSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsistencyEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyEventProcessor.class);

    private final ObjectMapper objectMapper;
    private final VehicleSnapshotRepository vehicleRepository;
    private final MaintenanceSnapshotRepository maintenanceRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InconsistencyReconciler reconciler;

    @Value("${app.kafka.consumer-group}")
    private String consumerName;

    @Transactional
    public void process(String eventJson) {
        VehicleMaintenanceEvent event = readEvent(eventJson);
        ProcessedEventId id = new ProcessedEventId(event.eventId(), consumerName);
        if (processedEventRepository.existsById(id)) {
            log.info("Duplicate source event ignored eventId={} eventType={} vehicleId={}",
                    event.eventId(), event.eventType(), event.vehicleId());
            return;
        }

        if (event.eventVersion() == 1) {
            apply(event);
        } else {
            log.warn("Unsupported source event version ignored eventId={} version={}",
                    event.eventId(), event.eventVersion());
        }

        processedEventRepository.save(new ProcessedEventEntity(
                event.eventId(), consumerName, OffsetDateTime.now(ZoneOffset.UTC)
        ));
    }

    private void apply(VehicleMaintenanceEvent event) {
        switch (event.eventType()) {
            case "VehicleCreated", "VehicleUpdated" -> upsertVehicle(event);
            case "VehicleDeleted" -> deleteVehicle(event);
            case "MaintenanceCreated", "MaintenanceUpdated" -> upsertMaintenance(event);
            case "MaintenanceDeleted" -> deleteMaintenance(event);
            default -> log.debug("Source event does not affect consistency eventType={}", event.eventType());
        }
    }

    private void upsertVehicle(VehicleMaintenanceEvent event) {
        VehicleSnapshotEntity vehicle = vehicleRepository.findById(event.vehicleId())
                .orElseGet(() -> new VehicleSnapshotEntity(event.vehicleId(), event.occurredAt()));
        vehicle.update(requiredInteger(event.payload(), "manufactureYear"), event.occurredAt());
        vehicleRepository.save(vehicle);
        reconciler.reconcile(event.vehicleId(), event.correlationId());
    }

    private void deleteVehicle(VehicleMaintenanceEvent event) {
        reconciler.resolveAll(event.vehicleId(), event.correlationId());
        maintenanceRepository.deleteAllByVehicleId(event.vehicleId());
        vehicleRepository.deleteById(event.vehicleId());
    }

    private void upsertMaintenance(VehicleMaintenanceEvent event) {
        vehicleRepository.findById(event.vehicleId()).orElseGet(() -> vehicleRepository.save(
                new VehicleSnapshotEntity(event.vehicleId(), event.occurredAt())
        ));
        MaintenanceSnapshotEntity maintenance = maintenanceRepository.findById(event.aggregateId())
                .orElseGet(() -> new MaintenanceSnapshotEntity(event.aggregateId(), event.vehicleId()));
        maintenance.update(
                LocalDate.parse(requiredText(event.payload(), "maintenanceDate")),
                requiredInteger(event.payload(), "odometer"),
                requiredText(event.payload(), "description"),
                event.occurredAt()
        );
        maintenanceRepository.save(maintenance);
        reconciler.reconcile(event.vehicleId(), event.correlationId());
    }

    private void deleteMaintenance(VehicleMaintenanceEvent event) {
        maintenanceRepository.deleteById(event.aggregateId());
        reconciler.reconcile(event.vehicleId(), event.correlationId());
    }

    private VehicleMaintenanceEvent readEvent(String json) {
        try {
            VehicleMaintenanceEvent event = objectMapper.readValue(json, VehicleMaintenanceEvent.class);
            if (event.eventId() == null || event.eventType() == null || event.vehicleId() == null
                    || event.occurredAt() == null || event.payload() == null) {
                throw new IllegalArgumentException("Source event contains missing required fields");
            }
            return event;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid vehicle maintenance event", exception);
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing event payload field: " + field);
        }
        return value.asText();
    }

    private Integer requiredInteger(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isIntegralNumber()) {
            throw new IllegalArgumentException("Missing event payload field: " + field);
        }
        return value.intValue();
    }
}
