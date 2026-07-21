package com.mmetzner.vmh.inconsistency.application;

import com.mmetzner.vmh.inconsistency.application.dto.MaintenanceInconsistencyResponse;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyEntity;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyRepository;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MaintenanceInconsistencyService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final VehicleRepository vehicleRepository;
    private final MaintenanceInconsistencyRepository inconsistencyRepository;

    @Transactional(readOnly = true)
    public List<MaintenanceInconsistencyResponse> list(
            UUID ownerId,
            UUID vehicleId,
            boolean includeResolved
    ) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        List<MaintenanceInconsistencyEntity> inconsistencies = includeResolved
                ? inconsistencyRepository.findAllByVehicleIdOrderByDetectedAtDesc(vehicleId)
                : inconsistencyRepository.findAllByVehicleIdAndStatusOrderByDetectedAtDesc(vehicleId, ACTIVE_STATUS);

        return inconsistencies.stream()
                .map(this::toResponse)
                .toList();
    }

    private void ensureVehicleBelongsToOwner(UUID ownerId, UUID vehicleId) {
        vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.VEHICLE_NOT_FOUND,
                        ApiMessages.Vehicles.NOT_FOUND
                ));
    }

    private MaintenanceInconsistencyResponse toResponse(MaintenanceInconsistencyEntity inconsistency) {
        List<UUID> maintenanceIds = StreamSupport.stream(
                        inconsistency.getMaintenanceIds().spliterator(),
                        false
                )
                .map(node -> UUID.fromString(node.asText()))
                .toList();

        return new MaintenanceInconsistencyResponse(
                inconsistency.getAlertId(),
                inconsistency.getRule(),
                inconsistency.getSeverity(),
                maintenanceIds,
                inconsistency.getSummary(),
                inconsistency.getDetails(),
                inconsistency.getStatus(),
                inconsistency.getDetectedAt(),
                inconsistency.getResolvedAt()
        );
    }
}
