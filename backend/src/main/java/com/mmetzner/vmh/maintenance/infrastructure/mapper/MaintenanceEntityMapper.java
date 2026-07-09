package com.mmetzner.vmh.maintenance.infrastructure.mapper;

import com.mmetzner.vmh.maintenance.domain.model.Maintenance;
import com.mmetzner.vmh.maintenance.infrastructure.entity.MaintenanceEntity;
import com.mmetzner.vmh.vehicle.infrastructure.entity.VehicleEntity;

public final class MaintenanceEntityMapper {

    private MaintenanceEntityMapper() {
    }

    public static Maintenance toDomain(MaintenanceEntity entity) {
        return new Maintenance(
                entity.getId(),
                entity.getVehicle().getId(),
                entity.getMaintenanceDate(),
                entity.getOdometer(),
                entity.getDescription(),
                entity.getCost(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static MaintenanceEntity toEntity(Maintenance maintenance) {
        return new MaintenanceEntity(
                maintenance.id(),
                VehicleEntity.reference(maintenance.vehicleId()),
                maintenance.maintenanceDate(),
                maintenance.odometer(),
                maintenance.description(),
                maintenance.cost(),
                maintenance.createdAt(),
                maintenance.updatedAt()
        );
    }
}