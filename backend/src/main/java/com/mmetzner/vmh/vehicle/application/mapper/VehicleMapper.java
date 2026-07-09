package com.mmetzner.vmh.vehicle.application.mapper;

import com.mmetzner.vmh.vehicle.application.dto.VehicleResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleSummaryResponse;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.id(),
                vehicle.plate(),
                vehicle.brand(),
                vehicle.model(),
                vehicle.manufactureYear(),
                vehicle.color(),
                vehicle.createdAt(),
                vehicle.updatedAt()
        );
    }

    public VehicleSummaryResponse toSummaryResponse(Vehicle vehicle) {
        return new VehicleSummaryResponse(
                vehicle.id(),
                vehicle.plate(),
                vehicle.brand(),
                vehicle.model(),
                vehicle.manufactureYear(),
                vehicle.color()
        );
    }
}