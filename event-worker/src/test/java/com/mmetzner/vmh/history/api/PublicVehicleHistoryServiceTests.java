package com.mmetzner.vmh.history.api;

import com.mmetzner.vmh.history.projection.MaintenanceHistoryEntity;
import com.mmetzner.vmh.history.projection.MaintenanceHistoryRepository;
import com.mmetzner.vmh.history.projection.VehicleHistoryEntity;
import com.mmetzner.vmh.history.projection.VehicleHistoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicVehicleHistoryServiceTests {

    @Test
    void shouldReturnSanitizedChronologicalHistory() {
        VehicleHistoryRepository vehicleRepository = mock(VehicleHistoryRepository.class);
        MaintenanceHistoryRepository maintenanceRepository = mock(MaintenanceHistoryRepository.class);
        PublicVehicleHistoryService service = new PublicVehicleHistoryService(
                vehicleRepository,
                maintenanceRepository
        );
        UUID vehicleId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        VehicleHistoryEntity vehicle = new VehicleHistoryEntity(vehicleId);
        vehicle.updateDetails("Honda", "Civic", 2020, "Black", OffsetDateTime.now());
        vehicle.updateSharing(true, publicId, OffsetDateTime.now());
        MaintenanceHistoryEntity maintenance = new MaintenanceHistoryEntity(UUID.randomUUID(), vehicle);
        maintenance.update(
                LocalDate.of(2026, 7, 21),
                45_000,
                "Oil change",
                new BigDecimal("350.00"),
                OffsetDateTime.now()
        );
        when(vehicleRepository.findByPublicIdAndSharingEnabledTrue(publicId))
                .thenReturn(Optional.of(vehicle));
        when(maintenanceRepository
                .findAllByVehicleVehicleIdOrderByMaintenanceDateAscMaintenanceIdAsc(vehicleId))
                .thenReturn(List.of(maintenance));

        PublicVehicleHistoryResponse response = service.findByPublicId(publicId);

        assertThat(response.publicId()).isEqualTo(publicId);
        assertThat(response.brand()).isEqualTo("Honda");
        assertThat(response.maintenances()).singleElement().satisfies(item -> {
            assertThat(item.description()).isEqualTo("Oil change");
            assertThat(item.odometer()).isEqualTo(45_000);
        });
    }

    @Test
    void shouldHideMissingOrRevokedHistory() {
        VehicleHistoryRepository vehicleRepository = mock(VehicleHistoryRepository.class);
        MaintenanceHistoryRepository maintenanceRepository = mock(MaintenanceHistoryRepository.class);
        PublicVehicleHistoryService service = new PublicVehicleHistoryService(
                vehicleRepository,
                maintenanceRepository
        );
        UUID publicId = UUID.randomUUID();
        when(vehicleRepository.findByPublicIdAndSharingEnabledTrue(publicId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByPublicId(publicId))
                .isInstanceOf(PublicHistoryNotFoundException.class);
    }
}
