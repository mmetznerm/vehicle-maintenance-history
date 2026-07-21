package com.mmetzner.vmh.vehicle.infrastructure.mapper;

import com.mmetzner.vmh.auth.infrastructure.entity.UserEntity;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.infrastructure.entity.VehicleEntity;

public final class VehicleEntityMapper {

    private VehicleEntityMapper() {
    }

    public static Vehicle toDomain(VehicleEntity entity) {
        return new Vehicle(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getPlate(),
                entity.getBrand(),
                entity.getModel(),
                entity.getManufactureYear(),
                entity.getColor(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isHistorySharingEnabled(),
                entity.getHistoryPublicId()
        );
    }

    public static VehicleEntity toEntity(Vehicle vehicle) {
        return new VehicleEntity(
                vehicle.id(),
                UserEntity.reference(vehicle.ownerId()),
                vehicle.plate(),
                vehicle.brand(),
                vehicle.model(),
                vehicle.manufactureYear(),
                vehicle.color(),
                vehicle.createdAt(),
                vehicle.updatedAt(),
                vehicle.historySharingEnabled(),
                vehicle.historyPublicId()
        );
    }
}
