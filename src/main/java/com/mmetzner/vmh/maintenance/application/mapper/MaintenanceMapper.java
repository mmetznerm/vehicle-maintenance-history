package com.mmetzner.vmh.maintenance.application.mapper;

import com.mmetzner.vmh.maintenance.application.dto.MaintenanceResponse;
import com.mmetzner.vmh.maintenance.domain.model.Maintenance;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public MaintenanceResponse toResponse(Maintenance maintenance) {
        return new MaintenanceResponse(
                maintenance.id(),
                maintenance.vehicleId(),
                maintenance.maintenanceDate(),
                maintenance.odometer(),
                maintenance.description(),
                maintenance.cost(),
                maintenance.createdAt(),
                maintenance.updatedAt()
        );
    }
}