package com.mmetzner.vmh.consistency.detection;

import java.util.List;
import java.util.UUID;

public record DetectedInconsistency(
        UUID alertId,
        UUID vehicleId,
        InconsistencyRule rule,
        InconsistencySeverity severity,
        List<UUID> maintenanceIds,
        String summary,
        String details
) {
}
