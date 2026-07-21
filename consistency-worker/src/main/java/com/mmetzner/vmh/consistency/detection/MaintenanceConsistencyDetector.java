package com.mmetzner.vmh.consistency.detection;

import com.mmetzner.vmh.consistency.projection.MaintenanceSnapshotEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MaintenanceConsistencyDetector {

    public List<DetectedInconsistency> detect(
            UUID vehicleId,
            Integer manufactureYear,
            List<MaintenanceSnapshotEntity> maintenances
    ) {
        List<DetectedInconsistency> detected = new ArrayList<>();
        List<MaintenanceSnapshotEntity> ordered = maintenances.stream()
                .sorted(Comparator.comparing(MaintenanceSnapshotEntity::getMaintenanceDate)
                        .thenComparing(item -> item.getMaintenanceId().toString()))
                .toList();

        detectDatesBeforeManufacture(vehicleId, manufactureYear, ordered, detected);
        detectOdometerRollbacks(vehicleId, ordered, detected);
        detectDuplicates(vehicleId, ordered, detected);
        return detected;
    }

    private void detectDatesBeforeManufacture(
            UUID vehicleId,
            Integer manufactureYear,
            List<MaintenanceSnapshotEntity> maintenances,
            List<DetectedInconsistency> detected
    ) {
        if (manufactureYear == null) {
            return;
        }
        maintenances.stream()
                .filter(item -> item.getMaintenanceDate().getYear() < manufactureYear)
                .forEach(item -> detected.add(create(
                        vehicleId,
                        InconsistencyRule.DATE_BEFORE_MANUFACTURE,
                        InconsistencySeverity.CRITICAL,
                        List.of(item.getMaintenanceId()),
                        "Maintenance predates vehicle manufacture",
                        "Maintenance date %s is earlier than manufacture year %d."
                                .formatted(item.getMaintenanceDate(), manufactureYear)
                )));
    }

    private void detectOdometerRollbacks(
            UUID vehicleId,
            List<MaintenanceSnapshotEntity> maintenances,
            List<DetectedInconsistency> detected
    ) {
        MaintenanceSnapshotEntity highestPrevious = null;
        Map<LocalDate, List<MaintenanceSnapshotEntity>> byDate = maintenances.stream()
                .collect(Collectors.groupingBy(
                        MaintenanceSnapshotEntity::getMaintenanceDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<MaintenanceSnapshotEntity> sameDay : byDate.values()) {
            if (highestPrevious != null) {
                for (MaintenanceSnapshotEntity current : sameDay) {
                    if (current.getOdometer() < highestPrevious.getOdometer()) {
                        detected.add(create(
                                vehicleId,
                                InconsistencyRule.ODOMETER_ROLLBACK,
                                InconsistencySeverity.CRITICAL,
                                List.of(highestPrevious.getMaintenanceId(), current.getMaintenanceId()),
                                "Odometer reading decreased between maintenance records",
                                "Reading changed from %d km to %d km in chronological order."
                                        .formatted(highestPrevious.getOdometer(), current.getOdometer())
                        ));
                    }
                }
            }

            MaintenanceSnapshotEntity highestOnDay = sameDay.stream()
                    .max(Comparator.comparing(MaintenanceSnapshotEntity::getOdometer)
                            .thenComparing(item -> item.getMaintenanceId().toString()))
                    .orElseThrow();
            if (highestPrevious == null || highestOnDay.getOdometer() > highestPrevious.getOdometer()) {
                highestPrevious = highestOnDay;
            }
        }
    }

    private void detectDuplicates(
            UUID vehicleId,
            List<MaintenanceSnapshotEntity> maintenances,
            List<DetectedInconsistency> detected
    ) {
        maintenances.stream()
                .collect(Collectors.groupingBy(this::duplicateKey))
                .values().stream()
                .filter(group -> group.size() > 1)
                .forEach(group -> {
                    List<UUID> ids = group.stream()
                            .map(MaintenanceSnapshotEntity::getMaintenanceId)
                            .sorted(Comparator.comparing(UUID::toString))
                            .toList();
                    MaintenanceSnapshotEntity example = group.getFirst();
                    detected.add(create(
                            vehicleId,
                            InconsistencyRule.POSSIBLE_DUPLICATE,
                            InconsistencySeverity.WARNING,
                            ids,
                            "Possible duplicate maintenance records",
                            "%d records share date %s, odometer %d km and description."
                                    .formatted(group.size(), example.getMaintenanceDate(), example.getOdometer())
                    ));
                });
    }

    private String duplicateKey(MaintenanceSnapshotEntity maintenance) {
        String description = Normalizer.normalize(maintenance.getDescription(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return maintenance.getMaintenanceDate() + "|" + maintenance.getOdometer() + "|" + description;
    }

    private DetectedInconsistency create(
            UUID vehicleId,
            InconsistencyRule rule,
            InconsistencySeverity severity,
            List<UUID> maintenanceIds,
            String summary,
            String details
    ) {
        List<UUID> sortedIds = maintenanceIds.stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        String identity = vehicleId + "|" + rule + "|" + sortedIds;
        UUID alertId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
        return new DetectedInconsistency(
                alertId, vehicleId, rule, severity, sortedIds, summary, details
        );
    }
}
