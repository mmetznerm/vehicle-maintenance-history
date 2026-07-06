package com.mmetzner.vmh.vehicle.repository;

import com.mmetzner.vmh.vehicle.infrastructure.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepositoryJpa extends JpaRepository<VehicleEntity, UUID> {

    @Query("""
            select vehicle
            from VehicleEntity vehicle
            where vehicle.id = :id
              and vehicle.owner.id = :ownerId
            """)
    Optional<VehicleEntity> findByIdAndOwnerId(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    @Query("""
            select vehicle
            from VehicleEntity vehicle
            where vehicle.owner.id = :ownerId
            order by vehicle.createdAt desc
            """)
    List<VehicleEntity> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("""
            select count(vehicle) > 0
            from VehicleEntity vehicle
            where vehicle.owner.id = :ownerId
              and vehicle.plate = :plate
            """)
    boolean existsByOwnerIdAndPlate(
            @Param("ownerId") UUID ownerId,
            @Param("plate") String plate
    );
}