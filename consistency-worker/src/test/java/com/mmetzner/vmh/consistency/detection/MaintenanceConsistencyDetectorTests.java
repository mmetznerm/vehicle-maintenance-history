package com.mmetzner.vmh.consistency.detection;

import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceConsistencyDetectorTests {

    private final MaintenanceConsistencyDetector detector = new MaintenanceConsistencyDetector();
    private final UUID vehicleId = UUID.randomUUID();

    @Test
    void shouldDetectAllSupportedRules() {
        MaintenanceSnapshotEntity beforeManufacture = maintenance(
                "00000000-0000-0000-0000-000000000001", "2019-12-10", 10_000, "Oil change"
        );
        MaintenanceSnapshotEntity baseline = maintenance(
                "00000000-0000-0000-0000-000000000002", "2021-01-10", 30_000, "Brake service"
        );
        MaintenanceSnapshotEntity rollback = maintenance(
                "00000000-0000-0000-0000-000000000003", "2021-02-10", 25_000, "Tire rotation"
        );
        MaintenanceSnapshotEntity duplicate = maintenance(
                "00000000-0000-0000-0000-000000000004", "2021-02-10", 25_000, "Tíre   Rotation!"
        );

        var result = detector.detect(
                vehicleId,
                2020,
                List.of(beforeManufacture, baseline, rollback, duplicate)
        );

        assertThat(result).extracting(DetectedInconsistency::rule)
                .contains(
                        InconsistencyRule.DATE_BEFORE_MANUFACTURE,
                        InconsistencyRule.ODOMETER_ROLLBACK,
                        InconsistencyRule.POSSIBLE_DUPLICATE
                );
        assertThat(result).filteredOn(alert -> alert.rule() == InconsistencyRule.POSSIBLE_DUPLICATE)
                .singleElement()
                .satisfies(alert -> assertThat(alert.maintenanceIds()).containsExactly(
                        rollback.getMaintenanceId(), duplicate.getMaintenanceId()
                ));
    }

    @Test
    void shouldNotCompareOdometerReadingsFromTheSameDay() {
        var result = detector.detect(vehicleId, 2020, List.of(
                maintenance("00000000-0000-0000-0000-000000000001", "2024-01-10", 50_000, "First"),
                maintenance("00000000-0000-0000-0000-000000000002", "2024-01-10", 40_000, "Second")
        ));

        assertThat(result).noneMatch(alert -> alert.rule() == InconsistencyRule.ODOMETER_ROLLBACK);
    }

    @Test
    void shouldGenerateStableAlertIdsRegardlessOfInputOrder() {
        MaintenanceSnapshotEntity first = maintenance(
                "00000000-0000-0000-0000-000000000001", "2024-01-10", 50_000, "Oil change"
        );
        MaintenanceSnapshotEntity second = maintenance(
                "00000000-0000-0000-0000-000000000002", "2024-01-10", 50_000, "oil-change"
        );

        UUID firstRun = detector.detect(vehicleId, 2020, List.of(first, second)).getFirst().alertId();
        UUID secondRun = detector.detect(vehicleId, 2020, List.of(second, first)).getFirst().alertId();

        assertThat(secondRun).isEqualTo(firstRun);
    }

    @Test
    void shouldReturnNoAlertsForAConsistentHistory() {
        var result = detector.detect(vehicleId, 2020, List.of(
                maintenance("00000000-0000-0000-0000-000000000001", "2021-01-10", 20_000, "Oil change"),
                maintenance("00000000-0000-0000-0000-000000000002", "2022-01-10", 30_000, "Brake service")
        ));

        assertThat(result).isEmpty();
    }

    private MaintenanceSnapshotEntity maintenance(String id, String date, int odometer, String description) {
        MaintenanceSnapshotEntity entity = new MaintenanceSnapshotEntity(UUID.fromString(id), vehicleId);
        entity.update(LocalDate.parse(date), odometer, description, OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }
}
