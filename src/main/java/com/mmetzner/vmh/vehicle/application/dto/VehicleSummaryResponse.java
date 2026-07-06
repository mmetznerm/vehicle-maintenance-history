package com.mmetzner.vmh.vehicle.application.dto;

import java.util.UUID;

public record VehicleSummaryResponse(
        UUID id,
        String plate,
        String brand,
        String model,
        Integer manufactureYear,
        String color
) {
}