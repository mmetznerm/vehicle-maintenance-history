package com.mmetzner.vmh.consistency.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActiveInconsistencyRepository extends JpaRepository<ActiveInconsistencyEntity, UUID> {
    List<ActiveInconsistencyEntity> findAllByVehicleId(UUID vehicleId);
}
