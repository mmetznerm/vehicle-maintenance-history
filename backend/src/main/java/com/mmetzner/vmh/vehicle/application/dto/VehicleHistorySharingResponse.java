package com.mmetzner.vmh.vehicle.application.dto;

import java.util.UUID;

public record VehicleHistorySharingResponse(
        boolean enabled,
        UUID publicId
) {
}
