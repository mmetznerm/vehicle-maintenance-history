package com.mmetzner.vmh.inconsistency.application.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MaintenanceInconsistencyResponse(
        UUID alertId,
        String rule,
        String severity,
        List<UUID> maintenanceIds,
        String summary,
        String details,
        String status,
        OffsetDateTime detectedAt,
        OffsetDateTime resolvedAt
) {
}
