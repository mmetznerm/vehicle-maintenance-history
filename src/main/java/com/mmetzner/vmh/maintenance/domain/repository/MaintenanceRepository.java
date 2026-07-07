package com.mmetzner.vmh.maintenance.domain.repository;

import com.mmetzner.vmh.maintenance.domain.model.Maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRepository {

    Maintenance save(Maintenance maintenance);

    Optional<Maintenance> findByIdAndVehicleId(UUID id, UUID vehicleId);

    List<Maintenance> findAllByVehicleId(UUID vehicleId);

    boolean existsByVehicleId(UUID vehicleId);

    void delete(Maintenance maintenance);
}