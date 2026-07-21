package com.mmetzner.vmh.history.api;

import java.time.LocalDate;
import java.util.UUID;

public record PublicMaintenanceResponse(
        UUID id,
        LocalDate maintenanceDate,
        Integer odometer,
        String description
) {
}
