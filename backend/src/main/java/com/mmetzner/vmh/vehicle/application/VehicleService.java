package com.mmetzner.vmh.vehicle.application;

import com.mmetzner.vmh.auth.domain.repository.UserRepository;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.exception.ConflictException;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.UpdateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.VehicleResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleSummaryResponse;
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

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Transactional
    public void deleteVehicle(UUID ownerId, UUID vehicleId) {
        Vehicle vehicle = findVehicleByIdAndOwnerId(ownerId, vehicleId);

        vehicleRepository.delete(vehicle);
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
}