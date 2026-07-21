package com.mmetzner.vmh.history.api;

import com.mmetzner.vmh.history.projection.MaintenanceHistoryRepository;
import com.mmetzner.vmh.history.projection.VehicleHistoryEntity;
import com.mmetzner.vmh.history.projection.VehicleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicVehicleHistoryService {

    private final VehicleHistoryRepository vehicleRepository;
    private final MaintenanceHistoryRepository maintenanceRepository;

    @Transactional(readOnly = true)
    public PublicVehicleHistoryResponse findByPublicId(UUID publicId) {
        VehicleHistoryEntity vehicle = vehicleRepository.findByPublicIdAndSharingEnabledTrue(publicId)
                .orElseThrow(PublicHistoryNotFoundException::new);

        var maintenances = maintenanceRepository
                .findAllByVehicleVehicleIdOrderByMaintenanceDateAscMaintenanceIdAsc(vehicle.getVehicleId())
                .stream()
                .map(maintenance -> new PublicMaintenanceResponse(
                        maintenance.getMaintenanceId(),
                        maintenance.getMaintenanceDate(),
                        maintenance.getOdometer(),
                        maintenance.getDescription()
                ))
                .toList();

        return new PublicVehicleHistoryResponse(
                vehicle.getPublicId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufactureYear(),
                vehicle.getColor(),
                maintenances
        );
    }
}
