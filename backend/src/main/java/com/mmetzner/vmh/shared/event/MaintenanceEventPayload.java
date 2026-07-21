package com.mmetzner.vmh.shared.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MaintenanceEventPayload(
        LocalDate maintenanceDate,
        Integer odometer,
        String description,
        BigDecimal cost
) {
}
