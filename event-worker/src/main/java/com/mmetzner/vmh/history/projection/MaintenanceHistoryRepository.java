package com.mmetzner.vmh.history.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistoryEntity, UUID> {
    List<MaintenanceHistoryEntity> findAllByVehicleVehicleIdOrderByMaintenanceDateAscMaintenanceIdAsc(UUID vehicleId);
}
