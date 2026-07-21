package com.mmetzner.vmh.maintenance.application;

import com.mmetzner.vmh.maintenance.application.dto.CreateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.dto.MaintenanceResponse;
import com.mmetzner.vmh.maintenance.application.dto.UpdateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.mapper.MaintenanceMapper;
import com.mmetzner.vmh.maintenance.domain.model.Maintenance;
import com.mmetzner.vmh.maintenance.domain.repository.MaintenanceRepository;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.shared.event.EventType;
import com.mmetzner.vmh.shared.event.MaintenanceEventPayload;
import com.mmetzner.vmh.shared.event.OutboxEventWriter;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final OutboxEventWriter outboxEventWriter;

    @Transactional
    public MaintenanceResponse registerMaintenance(
            UUID ownerId,
            UUID vehicleId,
            CreateMaintenanceRequest request
    ) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        Maintenance maintenance = Maintenance.create(
                vehicleId,
                request.maintenanceDate(),
                request.odometer(),
                request.description(),
                request.cost()
        );

        Maintenance savedMaintenance = maintenanceRepository.save(maintenance);
        writeMaintenanceEvent(EventType.MAINTENANCE_CREATED, savedMaintenance);

        return maintenanceMapper.toResponse(savedMaintenance);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> listMaintenances(UUID ownerId, UUID vehicleId) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        return maintenanceRepository.findAllByVehicleId(vehicleId)
                .stream()
                .map(maintenanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse findMaintenance(UUID ownerId, UUID vehicleId, UUID maintenanceId) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        Maintenance maintenance = findMaintenanceByIdAndVehicleId(maintenanceId, vehicleId);

        return maintenanceMapper.toResponse(maintenance);
    }

    @Transactional
    public MaintenanceResponse updateMaintenance(
            UUID ownerId,
            UUID vehicleId,
            UUID maintenanceId,
            UpdateMaintenanceRequest request
    ) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        Maintenance currentMaintenance = findMaintenanceByIdAndVehicleId(maintenanceId, vehicleId);

        Maintenance updatedMaintenance = currentMaintenance.updateDetails(
                request.maintenanceDate(),
                request.odometer(),
                request.description(),
                request.cost()
        );

        Maintenance savedMaintenance = maintenanceRepository.save(updatedMaintenance);
        writeMaintenanceEvent(EventType.MAINTENANCE_UPDATED, savedMaintenance);

        return maintenanceMapper.toResponse(savedMaintenance);
    }

    @Transactional
    public void deleteMaintenance(UUID ownerId, UUID vehicleId, UUID maintenanceId) {
        ensureVehicleBelongsToOwner(ownerId, vehicleId);

        Maintenance maintenance = findMaintenanceByIdAndVehicleId(maintenanceId, vehicleId);

        outboxEventWriter.write(
                EventType.MAINTENANCE_DELETED,
                maintenance.id(),
                maintenance.vehicleId(),
                null
        );
        maintenanceRepository.delete(maintenance);
    }

    private Vehicle ensureVehicleBelongsToOwner(UUID ownerId, UUID vehicleId) {
        return vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.VEHICLE_NOT_FOUND,
                        ApiMessages.Vehicles.NOT_FOUND
                ));
    }

    private Maintenance findMaintenanceByIdAndVehicleId(UUID maintenanceId, UUID vehicleId) {
        return maintenanceRepository.findByIdAndVehicleId(maintenanceId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.MAINTENANCE_NOT_FOUND,
                        ApiMessages.Maintenances.NOT_FOUND
                ));
    }

    private void writeMaintenanceEvent(EventType type, Maintenance maintenance) {
        outboxEventWriter.write(
                type,
                maintenance.id(),
                maintenance.vehicleId(),
                new MaintenanceEventPayload(
                        maintenance.maintenanceDate(),
                        maintenance.odometer(),
                        maintenance.description(),
                        maintenance.cost()
                )
        );
    }
}
