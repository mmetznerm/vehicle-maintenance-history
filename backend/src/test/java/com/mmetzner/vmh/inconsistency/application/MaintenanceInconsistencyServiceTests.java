package com.mmetzner.vmh.inconsistency.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyEntity;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyRepository;
import com.mmetzner.vmh.shared.exception.ResourceNotFoundException;
import com.mmetzner.vmh.vehicle.domain.model.Vehicle;
import com.mmetzner.vmh.vehicle.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MaintenanceInconsistencyServiceTests {

    private VehicleRepository vehicleRepository;
    private MaintenanceInconsistencyRepository inconsistencyRepository;
    private MaintenanceInconsistencyService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        inconsistencyRepository = mock(MaintenanceInconsistencyRepository.class);
        service = new MaintenanceInconsistencyService(vehicleRepository, inconsistencyRepository);
    }

    @Test
    void shouldListOnlyActiveAlertsByDefault() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)).thenReturn(Optional.of(mock(Vehicle.class)));
        MaintenanceInconsistencyEntity entity = new MaintenanceInconsistencyEntity(UUID.randomUUID(), vehicleId);
        entity.detect(
                "ODOMETER_ROLLBACK", "CRITICAL",
                new ObjectMapper().readTree("[\"%s\"]".formatted(maintenanceId)),
                "Odometer decreased", "Reading changed from 50000 km to 40000 km.", OffsetDateTime.now()
        );
        when(inconsistencyRepository.findAllByVehicleIdAndStatusOrderByDetectedAtDesc(vehicleId, "ACTIVE"))
                .thenReturn(List.of(entity));

        var response = service.list(ownerId, vehicleId, false);

        assertThat(response).singleElement().satisfies(alert -> {
            assertThat(alert.rule()).isEqualTo("ODOMETER_ROLLBACK");
            assertThat(alert.maintenanceIds()).containsExactly(maintenanceId);
            assertThat(alert.status()).isEqualTo("ACTIVE");
        });
        verify(inconsistencyRepository, never()).findAllByVehicleIdOrderByDetectedAtDesc(vehicleId);
    }

    @Test
    void shouldIncludeResolvedAlertsWhenRequested() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)).thenReturn(Optional.of(mock(Vehicle.class)));
        when(inconsistencyRepository.findAllByVehicleIdOrderByDetectedAtDesc(vehicleId)).thenReturn(List.of());

        assertThat(service.list(ownerId, vehicleId, true)).isEmpty();

        verify(inconsistencyRepository).findAllByVehicleIdOrderByDetectedAtDesc(vehicleId);
    }

    @Test
    void shouldHideAlertsWhenVehicleDoesNotBelongToOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findByIdAndOwnerId(vehicleId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(ownerId, vehicleId, false))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(inconsistencyRepository);
    }
}
