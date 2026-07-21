package com.mmetzner.vmh.vehicle.domain.model;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Vehicle(
        UUID id,
        UUID ownerId,
        String plate,
        String brand,
        String model,
        Integer manufactureYear,
        String color,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean historySharingEnabled,
        UUID historyPublicId
) {

    public Vehicle {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(ownerId, "ownerId is required");

        plate = normalizePlate(plate);
        brand = requireNotBlank(brand, "brand is required");
        model = requireNotBlank(model, "model is required");

        if (manufactureYear == null) {
            throw new IllegalArgumentException("manufactureYear is required");
        }

        if (manufactureYear < 1886) {
            throw new IllegalArgumentException("manufactureYear must be greater than or equal to 1886");
        }

        if (color != null && color.isBlank()) {
            color = null;
        }

        if (historySharingEnabled && historyPublicId == null) {
            throw new IllegalArgumentException("historyPublicId is required when sharing is enabled");
        }

        if (!historySharingEnabled && historyPublicId != null) {
            throw new IllegalArgumentException("historyPublicId must be null when sharing is disabled");
        }
    }

    public Vehicle(
            UUID id,
            UUID ownerId,
            String plate,
            String brand,
            String model,
            Integer manufactureYear,
            String color,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, ownerId, plate, brand, model, manufactureYear, color, createdAt, updatedAt, false, null);
    }

    public static Vehicle create(
            UUID ownerId,
            String plate,
            String brand,
            String model,
            Integer manufactureYear,
            String color
    ) {
        return new Vehicle(
                UUID.randomUUID(),
                ownerId,
                plate,
                brand,
                model,
                manufactureYear,
                color,
                null,
                null,
                false,
                null
        );
    }

    public Vehicle updateDetails(
            String plate,
            String brand,
            String model,
            Integer manufactureYear,
            String color
    ) {
        return new Vehicle(
                id,
                ownerId,
                plate,
                brand,
                model,
                manufactureYear,
                color,
                createdAt,
                updatedAt,
                historySharingEnabled,
                historyPublicId
        );
    }

    public Vehicle enableHistorySharing() {
        if (historySharingEnabled) {
            return this;
        }

        return new Vehicle(
                id, ownerId, plate, brand, model, manufactureYear, color,
                createdAt, updatedAt, true, UUID.randomUUID()
        );
    }

    public Vehicle disableHistorySharing() {
        if (!historySharingEnabled) {
            return this;
        }

        return new Vehicle(
                id, ownerId, plate, brand, model, manufactureYear, color,
                createdAt, updatedAt, false, null
        );
    }

    private static String normalizePlate(String plate) {
        String normalizedPlate = requireNotBlank(plate, "plate is required");
        return normalizedPlate
                .replace("-", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
