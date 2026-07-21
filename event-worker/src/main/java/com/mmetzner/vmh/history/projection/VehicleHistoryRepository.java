package com.mmetzner.vmh.history.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleHistoryRepository extends JpaRepository<VehicleHistoryEntity, UUID> {
    Optional<VehicleHistoryEntity> findByPublicIdAndSharingEnabledTrue(UUID publicId);
}
