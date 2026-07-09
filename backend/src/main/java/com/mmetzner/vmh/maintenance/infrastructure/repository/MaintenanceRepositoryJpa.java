package com.mmetzner.vmh.maintenance.infrastructure.repository;

import com.mmetzner.vmh.maintenance.infrastructure.entity.MaintenanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRepositoryJpa extends JpaRepository<MaintenanceEntity, UUID> {

    @Query("""
            select maintenance
            from MaintenanceEntity maintenance
            where maintenance.id = :id
              and maintenance.vehicle.id = :vehicleId
            """)
    Optional<MaintenanceEntity> findByIdAndVehicleId(
            @Param("id") UUID id,
            @Param("vehicleId") UUID vehicleId
    );

    @Query("""
            select maintenance
            from MaintenanceEntity maintenance
            where maintenance.vehicle.id = :vehicleId
            order by maintenance.maintenanceDate desc, maintenance.createdAt desc
            """)
    List<MaintenanceEntity> findAllByVehicleId(@Param("vehicleId") UUID vehicleId);

    @Query("""
            select count(maintenance) > 0
            from MaintenanceEntity maintenance
            where maintenance.vehicle.id = :vehicleId
            """)
    boolean existsByVehicleId(@Param("vehicleId") UUID vehicleId);
}