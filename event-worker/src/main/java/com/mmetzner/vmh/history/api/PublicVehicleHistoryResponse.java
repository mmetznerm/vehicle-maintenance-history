package com.mmetzner.vmh.history.api;

import java.util.List;
import java.util.UUID;

public record PublicVehicleHistoryResponse(
        UUID publicId,
        String brand,
        String model,
        Integer manufactureYear,
        String color,
        List<PublicMaintenanceResponse> maintenances
) {
}
