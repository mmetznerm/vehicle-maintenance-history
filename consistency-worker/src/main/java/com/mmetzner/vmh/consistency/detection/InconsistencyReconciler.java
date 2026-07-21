package com.mmetzner.vmh.consistency.detection;

import com.mmetzner.vmh.consistency.outbox.AlertOutboxWriter;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyEntity;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyRepository;
import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotRepository;
import com.mmetzner.vmh.consistency.projection.VehicleSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InconsistencyReconciler {

    private final MaintenanceConsistencyDetector detector;
    private final VehicleSnapshotRepository vehicleRepository;
    private final MaintenanceSnapshotRepository maintenanceRepository;
    private final ActiveInconsistencyRepository activeRepository;
    private final AlertOutboxWriter outboxWriter;

    public void reconcile(UUID vehicleId, String correlationId) {
        Integer manufactureYear = vehicleRepository.findById(vehicleId)
                .map(vehicle -> vehicle.getManufactureYear())
                .orElse(null);
        Map<UUID, DetectedInconsistency> desired = detector.detect(
                        vehicleId,
                        manufactureYear,
                        maintenanceRepository.findAllByVehicleId(vehicleId)
                ).stream()
                .collect(Collectors.toMap(DetectedInconsistency::alertId, Function.identity()));
        Map<UUID, ActiveInconsistencyEntity> current = activeRepository.findAllByVehicleId(vehicleId)
                .stream()
                .collect(Collectors.toMap(ActiveInconsistencyEntity::getAlertId, Function.identity()));

        desired.values().forEach(alert -> {
            ActiveInconsistencyEntity existing = current.get(alert.alertId());
            if (existing == null || hasChanged(existing, alert)) {
                activeRepository.save(toEntity(alert));
                outboxWriter.detected(alert, correlationId);
            }
        });

        current.values().stream()
                .filter(alert -> !desired.containsKey(alert.getAlertId()))
                .forEach(alert -> {
                    outboxWriter.resolved(alert, correlationId);
                    activeRepository.delete(alert);
                });
    }

    public void resolveAll(UUID vehicleId, String correlationId) {
        activeRepository.findAllByVehicleId(vehicleId).forEach(alert -> {
            outboxWriter.resolved(alert, correlationId);
            activeRepository.delete(alert);
        });
    }

    private boolean hasChanged(ActiveInconsistencyEntity existing, DetectedInconsistency desired) {
        return !existing.getSeverity().equals(desired.severity().name())
                || !existing.getSummary().equals(desired.summary())
                || !existing.getDetails().equals(desired.details());
    }

    private ActiveInconsistencyEntity toEntity(DetectedInconsistency alert) {
        String maintenanceIds = alert.maintenanceIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
        return new ActiveInconsistencyEntity(
                alert.alertId(), alert.vehicleId(), alert.rule().name(), alert.severity().name(),
                maintenanceIds, alert.summary(), alert.details(), OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
