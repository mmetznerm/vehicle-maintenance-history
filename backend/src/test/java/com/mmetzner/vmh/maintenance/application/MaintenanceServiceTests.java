package com.mmetzner.vmh.maintenance.application;

import com.mmetzner.vmh.maintenance.application.dto.CreateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.dto.UpdateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.mapper.MaintenanceMapper;
import com.mmetzner.vmh.maintenance.domain.model.Maintenance;
import com.mmetzner.vmh.maintenance.domain.repository.MaintenanceRepository;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceServiceTests {

    private VehicleRepository vehicleRepository;
    private MaintenanceRepository maintenanceRepository;
    private MaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        maintenanceRepository = mock(MaintenanceRepository.class);

        maintenanceService = new MaintenanceService(
                vehicleRepository,
                maintenanceRepository,
                new MaintenanceMapper()
        );
    }

    @Test
    void shouldRegisterMaintenance() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(vehicle(vehicleId, ownerId)));

        when(maintenanceRepository.save(any(Maintenance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMaintenanceRequest request = new CreateMaintenanceRequest(
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00")
        );

        var response = maintenanceService.registerMaintenance(ownerId, vehicleId, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        assertThat(response.description()).isEqualTo("Troca de óleo");

        verify(maintenanceRepository).save(any(Maintenance.class));
    }

    @Test
    void shouldRejectMaintenanceWhenVehicleDoesNotBelongToOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.empty());

        CreateMaintenanceRequest request = new CreateMaintenanceRequest(
                LocalDate.now(),
                10_000,
                "Alinhamento",
                new BigDecimal("120.00")
        );

        assertThatThrownBy(() -> maintenanceService.registerMaintenance(ownerId, vehicleId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void shouldListMaintenances() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(vehicle(vehicleId, ownerId)));

        Maintenance maintenance = Maintenance.create(
                vehicleId,
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00")
        );

        when(maintenanceRepository.findAllByVehicleId(vehicleId))
                .thenReturn(List.of(maintenance));

        var response = maintenanceService.listMaintenances(ownerId, vehicleId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().vehicleId()).isEqualTo(vehicleId);
        assertThat(response.getFirst().description()).isEqualTo("Troca de óleo");
    }

    @Test
    void shouldUpdateMaintenance() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        Maintenance currentMaintenance = new Maintenance(
                maintenanceId,
                vehicleId,
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00"),
                null,
                null
        );

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(vehicle(vehicleId, ownerId)));

        when(maintenanceRepository.findByIdAndVehicleId(maintenanceId, vehicleId))
                .thenReturn(Optional.of(currentMaintenance));

        when(maintenanceRepository.save(any(Maintenance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMaintenanceRequest request = new UpdateMaintenanceRequest(
                LocalDate.of(2026, 7, 8),
                36_000,
                "Troca de óleo e filtro",
                new BigDecimal("300.00")
        );

        var response = maintenanceService.updateMaintenance(ownerId, vehicleId, maintenanceId, request);

        assertThat(response.id()).isEqualTo(maintenanceId);
        assertThat(response.odometer()).isEqualTo(36_000);
        assertThat(response.description()).isEqualTo("Troca de óleo e filtro");
        assertThat(response.cost()).isEqualByComparingTo("300.00");
    }

    @Test
    void shouldDeleteMaintenance() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        Maintenance maintenance = new Maintenance(
                maintenanceId,
                vehicleId,
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00"),
                null,
                null
        );

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(vehicle(vehicleId, ownerId)));

        when(maintenanceRepository.findByIdAndVehicleId(maintenanceId, vehicleId))
                .thenReturn(Optional.of(maintenance));

        maintenanceService.deleteMaintenance(ownerId, vehicleId, maintenanceId);

        verify(maintenanceRepository).delete(maintenance);
    }

    @Test
    void shouldRejectWhenMaintenanceDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId))
                .thenReturn(Optional.of(vehicle(vehicleId, ownerId)));

        when(maintenanceRepository.findByIdAndVehicleId(maintenanceId, vehicleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.findMaintenance(ownerId, vehicleId, maintenanceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Vehicle vehicle(UUID vehicleId, UUID ownerId) {
        return new Vehicle(
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
    }
}