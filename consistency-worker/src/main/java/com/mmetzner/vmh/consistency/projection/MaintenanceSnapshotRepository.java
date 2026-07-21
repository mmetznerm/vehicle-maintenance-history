package com.mmetzner.vmh.consistency.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceSnapshotRepository extends JpaRepository<MaintenanceSnapshotEntity, UUID> {
    List<MaintenanceSnapshotEntity> findAllByVehicleId(UUID vehicleId);
    void deleteAllByVehicleId(UUID vehicleId);
}
