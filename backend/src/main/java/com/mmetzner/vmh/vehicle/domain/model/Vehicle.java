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
        OffsetDateTime updatedAt
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
                updatedAt
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