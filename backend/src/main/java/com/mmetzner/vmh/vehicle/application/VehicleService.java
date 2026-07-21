package com.mmetzner.vmh.vehicle.application;

import com.mmetzner.vmh.auth.domain.repository.UserRepository;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.exception.ConflictException;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.shared.event.EventType;
import com.mmetzner.vmh.shared.event.OutboxEventWriter;
import com.mmetzner.vmh.shared.event.VehicleEventPayload;
import com.mmetzner.vmh.shared.event.HistorySharingEventPayload;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.UpdateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.VehicleResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleSummaryResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleHistorySharingResponse;
import com.mmetzner.vmh.vehicle.application.mapper.VehicleMapper;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final OutboxEventWriter outboxEventWriter;

    @Transactional
    public VehicleResponse registerVehicle(UUID ownerId, CreateVehicleRequest request) {
        ensureUserExists(ownerId);

        Vehicle vehicle = Vehicle.create(
                ownerId,
                request.plate(),
                request.brand(),
                request.model(),
                request.manufactureYear(),
                request.color()
        );

        ensurePlateIsAvailable(ownerId, vehicle.plate());

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        writeVehicleEvent(EventType.VEHICLE_CREATED, savedVehicle);

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleSummaryResponse> listVehicles(UUID ownerId) {
        return vehicleRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(vehicleMapper::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse findVehicle(UUID ownerId, UUID vehicleId) {
        Vehicle vehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);

        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID ownerId, UUID vehicleId, UpdateVehicleRequest request) {
        Vehicle currentVehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);

        Vehicle updatedVehicle = currentVehicle.updateDetails(
                request.plate(),
                request.brand(),
                request.model(),
                request.manufactureYear(),
                request.color()
        );

        if (!currentVehicle.plate().equals(updatedVehicle.plate())) {
            ensurePlateIsAvailable(ownerId, updatedVehicle.plate());
        }

        Vehicle savedVehicle = vehicleRepository.save(updatedVehicle);
        writeVehicleEvent(EventType.VEHICLE_UPDATED, savedVehicle);

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Transactional
    public void deleteVehicle(UUID ownerId, UUID vehicleId) {
        Vehicle vehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);

        outboxEventWriter.write(EventType.VEHICLE_DELETED, vehicle.id(), vehicle.id(), null);
        vehicleRepository.delete(vehicle);
    }

    @Transactional(readOnly = true)
    public VehicleHistorySharingResponse getHistorySharing(UUID ownerId, UUID vehicleId) {
        Vehicle vehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);
        return toHistorySharingResponse(vehicle);
    }

    @Transactional
    public VehicleHistorySharingResponse enableHistorySharing(UUID ownerId, UUID vehicleId) {
        Vehicle currentVehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);
        Vehicle enabledVehicle = currentVehicle.enableHistorySharing();

        if (enabledVehicle != currentVehicle) {
            enabledVehicle = vehicleRepository.save(enabledVehicle);
            writeVehicleEvent(EventType.VEHICLE_UPDATED, enabledVehicle);
            writeHistorySharingEvent(enabledVehicle);
        }

        return toHistorySharingResponse(enabledVehicle);
    }

    @Transactional
    public void disableHistorySharing(UUID ownerId, UUID vehicleId) {
        Vehicle currentVehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);
        Vehicle disabledVehicle = currentVehicle.disableHistorySharing();

        if (disabledVehicle != currentVehicle) {
            disabledVehicle = vehicleRepository.save(disabledVehicle);
            writeHistorySharingEvent(disabledVehicle);
        }
    }

    private void ensureUserExists(UUID ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.USER_NOT_FOUND,
                        ApiMessages.Users.NOT_FOUND
                ));
    }

    private void ensurePlateIsAvailable(UUID ownerId, String plate) {
        if (vehicleRepository.existsByOwnerIdAndPlate(ownerId, plate)) {
            throw new ConflictException(
                    ApiErrorCode.VEHICLE_ALREADY_REGISTERED,
                    ApiMessages.Vehicles.ALREADY_REGISTERED_FOR_USER
            );
        }
    }

    private Vehicle findVehicleByIdAndOwnerId(UUID ownerId, UUID vehicleId) {
        return vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCode.VEHICLE_NOT_FOUND,
                        ApiMessages.Vehicles.NOT_FOUND
                ));
    }

    private void writeVehicleEvent(EventType type, Vehicle vehicle) {
        outboxEventWriter.write(
                type,
                vehicle.id(),
                vehicle.id(),
                new VehicleEventPayload(
                        vehicle.brand(),
                        vehicle.model(),
                        vehicle.manufactureYear(),
                        vehicle.color()
                )
        );
    }

    private void writeHistorySharingEvent(Vehicle vehicle) {
        outboxEventWriter.write(
                EventType.VEHICLE_HISTORY_SHARING_CHANGED,
                vehicle.id(),
                vehicle.id(),
                new HistorySharingEventPayload(
                        vehicle.historySharingEnabled(),
                        vehicle.historyPublicId()
                )
        );
    }

    private VehicleHistorySharingResponse toHistorySharingResponse(Vehicle vehicle) {
        return new VehicleHistorySharingResponse(
                vehicle.historySharingEnabled(),
                vehicle.historyPublicId()
        );
    }
}
