package com.mmetzner.vmh.maintenance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceResponse(
        UUID id,
        UUID vehicleId,
        LocalDate maintenanceDate,
        Integer odometer,
        String description,
        BigDecimal cost,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}