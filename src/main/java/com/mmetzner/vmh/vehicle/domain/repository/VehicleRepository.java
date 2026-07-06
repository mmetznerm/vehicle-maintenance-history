package com.mmetzner.vmh.vehicle.domain.repository;

import com.mmetzner.vmh.vehicle.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Vehicle> findAllByOwnerId(UUID ownerId);

    boolean existsByOwnerIdAndPlate(UUID ownerId, String plate);

    void delete(Vehicle vehicle);
}