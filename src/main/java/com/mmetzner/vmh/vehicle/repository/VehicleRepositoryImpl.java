package com.mmetzner.vmh.vehicle.repository;

import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import com.mmetzner.vmh.vehicle.infrastructure.entity.VehicleEntity;
import com.mmetzner.vmh.vehicle.infrastructure.mapper.VehicleEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VehicleRepositoryImpl implements VehicleRepository {

    private final VehicleRepositoryJpa vehicleRepositoryJpa;

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleEntity entity = VehicleEntityMapper.toEntity(vehicle);
        VehicleEntity savedEntity = vehicleRepositoryJpa.save(entity);

        return VehicleEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Vehicle> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return vehicleRepositoryJpa.findByIdAndOwnerId(id, ownerId)
                .map(VehicleEntityMapper::toDomain);
    }

    @Override
    public List<Vehicle> findAllByOwnerId(UUID ownerId) {
        return vehicleRepositoryJpa.findAllByOwnerId(ownerId)
                .stream()
                .map(VehicleEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOwnerIdAndPlate(UUID ownerId, String plate) {
        return vehicleRepositoryJpa.existsByOwnerIdAndPlate(ownerId, plate);
    }

    @Override
    public void delete(Vehicle vehicle) {
        VehicleEntity entity = VehicleEntityMapper.toEntity(vehicle);
        vehicleRepositoryJpa.delete(entity);
    }
}