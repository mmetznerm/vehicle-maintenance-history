package com.mmetzner.vmh.vehicle.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String plate,
        String brand,
        String model,
        Integer manufactureYear,
        String color,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}