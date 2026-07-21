package com.mmetzner.vmh.inconsistency.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceInconsistencyRepository extends JpaRepository<MaintenanceInconsistencyEntity, UUID> {
    List<MaintenanceInconsistencyEntity> findAllByVehicleIdOrderByDetectedAtDesc(UUID vehicleId);
    List<MaintenanceInconsistencyEntity> findAllByVehicleIdAndStatusOrderByDetectedAtDesc(UUID vehicleId, String status);
}
