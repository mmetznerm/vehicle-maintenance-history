package com.mmetzner.vmh.maintenance.infrastructure.repository;

import com.mmetzner.vmh.maintenance.domain.model.Maintenance;
import com.mmetzner.vmh.maintenance.domain.repository.MaintenanceRepository;
import com.mmetzner.vmh.maintenance.infrastructure.entity.MaintenanceEntity;
import com.mmetzner.vmh.maintenance.infrastructure.mapper.MaintenanceEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MaintenanceRepositoryImpl implements MaintenanceRepository {

    private final MaintenanceRepositoryJpa maintenanceRepositoryJpa;

    @Override
    public Maintenance save(Maintenance maintenance) {
        MaintenanceEntity entity = MaintenanceEntityMapper.toEntity(maintenance);
        MaintenanceEntity savedEntity = maintenanceRepositoryJpa.save(entity);

        return MaintenanceEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Maintenance> findByIdAndVehicleId(UUID id, UUID vehicleId) {
        return maintenanceRepositoryJpa.findByIdAndVehicleId(id, vehicleId)
                .map(MaintenanceEntityMapper::toDomain);
    }

    @Override
    public List<Maintenance> findAllByVehicleId(UUID vehicleId) {
        return maintenanceRepositoryJpa.findAllByVehicleId(vehicleId)
                .stream()
                .map(MaintenanceEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByVehicleId(UUID vehicleId) {
        return maintenanceRepositoryJpa.existsByVehicleId(vehicleId);
    }

    @Override
    public void delete(Maintenance maintenance) {
        MaintenanceEntity entity = MaintenanceEntityMapper.toEntity(maintenance);
        maintenanceRepositoryJpa.delete(entity);
    }
}