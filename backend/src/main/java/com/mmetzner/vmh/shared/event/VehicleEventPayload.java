package com.mmetzner.vmh.shared.event;

public record VehicleEventPayload(
        String brand,
        String model,
        Integer manufactureYear,
        String color
) {
}
