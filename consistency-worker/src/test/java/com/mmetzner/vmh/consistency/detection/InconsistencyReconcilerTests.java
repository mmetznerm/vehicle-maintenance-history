package com.mmetzner.vmh.consistency.detection;

import com.mmetzner.vmh.consistency.outbox.AlertOutboxWriter;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyEntity;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyRepository;
import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotRepository;
import com.mmetzner.vmh.consistency.projection.VehicleSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InconsistencyReconcilerTests {

    private MaintenanceConsistencyDetector detector;
    private VehicleSnapshotRepository vehicleRepository;
    private MaintenanceSnapshotRepository maintenanceRepository;
    private ActiveInconsistencyRepository activeRepository;
    private AlertOutboxWriter outboxWriter;
    private InconsistencyReconciler reconciler;

    @BeforeEach
    void setUp() {
        detector = mock(MaintenanceConsistencyDetector.class);
        vehicleRepository = mock(VehicleSnapshotRepository.class);
        maintenanceRepository = mock(MaintenanceSnapshotRepository.class);
        activeRepository = mock(ActiveInconsistencyRepository.class);
        outboxWriter = mock(AlertOutboxWriter.class);
        reconciler = new InconsistencyReconciler(
                detector, vehicleRepository, maintenanceRepository, activeRepository, outboxWriter
        );
    }

    @Test
    void shouldPublishOnlyNewDetections() {
        UUID vehicleId = UUID.randomUUID();
        DetectedInconsistency detected = alert(vehicleId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());
        when(maintenanceRepository.findAllByVehicleId(vehicleId)).thenReturn(List.of());
        when(detector.detect(vehicleId, null, List.of())).thenReturn(List.of(detected));
        when(activeRepository.findAllByVehicleId(vehicleId)).thenReturn(List.of());

        reconciler.reconcile(vehicleId, "correlation-id");

        verify(activeRepository).save(any(ActiveInconsistencyEntity.class));
        verify(outboxWriter).detected(detected, "correlation-id");
        verify(outboxWriter, never()).resolved(any(), any());
    }

    @Test
    void shouldResolveAlertsThatAreNoLongerDetected() {
        UUID vehicleId = UUID.randomUUID();
        DetectedInconsistency detected = alert(vehicleId);
        ActiveInconsistencyEntity active = new ActiveInconsistencyEntity(
                detected.alertId(), vehicleId, detected.rule().name(), detected.severity().name(),
                detected.maintenanceIds().getFirst().toString(), detected.summary(), detected.details(),
                OffsetDateTime.now()
        );
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());
        when(maintenanceRepository.findAllByVehicleId(vehicleId)).thenReturn(List.of());
        when(detector.detect(vehicleId, null, List.of())).thenReturn(List.of());
        when(activeRepository.findAllByVehicleId(vehicleId)).thenReturn(List.of(active));

        reconciler.reconcile(vehicleId, "correlation-id");

        verify(outboxWriter).resolved(active, "correlation-id");
        verify(activeRepository).delete(active);
    }

    private DetectedInconsistency alert(UUID vehicleId) {
        return new DetectedInconsistency(
                UUID.randomUUID(), vehicleId, InconsistencyRule.ODOMETER_ROLLBACK,
                InconsistencySeverity.CRITICAL, List.of(UUID.randomUUID()), "Summary", "Details"
        );
    }
}
