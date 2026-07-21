package com.mmetzner.vmh.vehicle.application;

import com.mmetzner.vmh.auth.domain.model.User;
import com.mmetzner.vmh.auth.domain.repository.UserRepository;
import com.mmetzner.vmh.shared.exception.ConflictException;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.shared.event.OutboxEventWriter;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.UpdateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.mapper.VehicleMapper;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleServiceTests {

    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private OutboxEventWriter outboxEventWriter;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        outboxEventWriter = mock(OutboxEventWriter.class);
        vehicleService = new VehicleService(
                userRepository,
                vehicleRepository,
                new VehicleMapper(),
                outboxEventWriter
        );
    }

    @Test
    void shouldRegisterVehicle() {
        UUID ownerId = UUID.randomUUID();
        CreateVehicleRequest request = new CreateVehicleRequest(
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                "Prata"
        );

        when(userRepository.findById(ownerId))
                .thenReturn(Optional.of(new User(ownerId, "Maycon", "maycon@email.com", "hash")));

        when(vehicleRepository.existsByOwnerIdAndPlate(ownerId, "ABC1234"))
                .thenReturn(false);

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = vehicleService.registerVehicle(ownerId, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.plate()).isEqualTo("ABC1234");
        assertThat(response.brand()).isEqualTo("Honda");

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());

        assertThat(vehicleCaptor.getValue().ownerId()).isEqualTo(ownerId);
    }

    @Test
    void shouldRejectVehicleWhenUserDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        CreateVehicleRequest request = new CreateVehicleRequest(
                "ABC1234",
                "Honda",
                "Civic",
                2020,
                null
        );

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.registerVehicle(ownerId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicatedPlateForSameUser() {
        UUID ownerId = UUID.randomUUID();
        CreateVehicleRequest request = new CreateVehicleRequest(
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                null
        );

        when(userRepository.findById(ownerId))
                .thenReturn(Optional.of(new User(ownerId, "Maycon", "maycon@email.com", "hash")));

        when(vehicleRepository.existsByOwnerIdAndPlate(ownerId, "ABC1234"))
                .thenReturn(true);

        assertThatThrownBy(() -> vehicleService.registerVehicle(ownerId, request))
                .isInstanceOf(ConflictException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void shouldListVehiclesFromOwner() {
        UUID ownerId = UUID.randomUUID();

        Vehicle vehicle = Vehicle.create(
                ownerId,
                "ABC1234",
                "Toyota",
                "Corolla",
                2022,
                "Preto"
        );

        when(vehicleRepository.findAllByOwnerId(ownerId)).thenReturn(List.of(vehicle));

        var response = vehicleService.listVehicles(ownerId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().plate()).isEqualTo("ABC1234");
    }

    @Test
    void shouldUpdateVehicle() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Vehicle currentVehicle = new Vehicle(
                vehicleId,
                ownerId,
                "ABC1234",
                "Honda",
                "Civic",
                2020,
                "Prata",
                null,
                null
        );

        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "XYZ9876",
                "Honda",
                "HR-V",
                2024,
                "Cinza"
        );

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(currentVehicle));

        when(vehicleRepository.existsByOwnerIdAndPlate(ownerId, "XYZ9876"))
                .thenReturn(false);

        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = vehicleService.updateVehicle(ownerId, vehicleId, request);

        assertThat(response.id()).isEqualTo(vehicleId);
        assertThat(response.plate()).isEqualTo("XYZ9876");
        assertThat(response.model()).isEqualTo("HR-V");
    }

    @Test
    void shouldDeleteVehicle() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Vehicle vehicle = Vehicle.create(
                ownerId,
                "ABC1234",
                "Toyota",
                "Corolla",
                2022,
                null
        );

        Vehicle persistedVehicle = new Vehicle(
                vehicleId,
                vehicle.ownerId(),
                vehicle.plate(),
                vehicle.brand(),
                vehicle.model(),
                vehicle.manufactureYear(),
                vehicle.color(),
                vehicle.createdAt(),
                vehicle.updatedAt()
        );

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(persistedVehicle));

        vehicleService.deleteVehicle(ownerId, vehicleId);

        verify(vehicleRepository).delete(persistedVehicle);
    }

    @Test
    void shouldRejectWhenVehicleDoesNotBelongToOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.findVehicle(ownerId, vehicleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
