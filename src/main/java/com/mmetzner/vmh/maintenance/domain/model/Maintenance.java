package com.mmetzner.vmh.maintenance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record Maintenance(
        UUID id,
        UUID vehicleId,
        LocalDate maintenanceDate,
        Integer odometer,
        String description,
        BigDecimal cost,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public Maintenance {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(vehicleId, "vehicleId is required");
        Objects.requireNonNull(maintenanceDate, "maintenanceDate is required");

        if (odometer == null) {
            throw new IllegalArgumentException("odometer is required");
        }

        if (odometer < 0) {
            throw new IllegalArgumentException("odometer must be greater than or equal to zero");
        }

        description = requireNotBlank(description, "description is required");

        if (cost == null) {
            throw new IllegalArgumentException("cost is required");
        }

        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost must be greater than or equal to zero");
        }
    }

    public static Maintenance create(
            UUID vehicleId,
            LocalDate maintenanceDate,
            Integer odometer,
            String description,
            BigDecimal cost
    ) {
        return new Maintenance(
                UUID.randomUUID(),
                vehicleId,
                maintenanceDate,
                odometer,
                description,
                cost,
                null,
                null
        );
    }

    public Maintenance updateDetails(
            LocalDate maintenanceDate,
            Integer odometer,
            String description,
            BigDecimal cost
    ) {
        return new Maintenance(
                id,
                vehicleId,
                maintenanceDate,
                odometer,
                description,
                cost,
                createdAt,
                updatedAt
        );
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}