package com.mmetzner.vmh.vehicle.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleTests {

    @Test
    void shouldCreateVehicleWithNormalizedPlate() {
        Vehicle vehicle = Vehicle.create(
                UUID.randomUUID(),
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                "Prata"
        );

        assertThat(vehicle.id()).isNotNull();
        assertThat(vehicle.plate()).isEqualTo("ABC1234");
        assertThat(vehicle.brand()).isEqualTo("Honda");
        assertThat(vehicle.model()).isEqualTo("Civic");
        assertThat(vehicle.manufactureYear()).isEqualTo(2020);
        assertThat(vehicle.color()).isEqualTo("Prata");
    }

    @Test
    void shouldRejectInvalidManufactureYear() {
        assertThatThrownBy(() -> Vehicle.create(
                UUID.randomUUID(),
                "ABC1234",
                "Ford",
                "Model T",
                1800,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("manufactureYear must be greater than or equal to 1886");
    }

    @Test
    void shouldRejectBlankPlate() {
        assertThatThrownBy(() -> Vehicle.create(
                UUID.randomUUID(),
                " ",
                "Toyota",
                "Corolla",
                2022,
                "Preto"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plate is required");
    }

    @Test
    void shouldGenerateAndRevokePublicHistoryIdentifiers() {
        Vehicle vehicle = Vehicle.create(
                UUID.randomUUID(), "ABC1234", "Toyota", "Corolla", 2022, "Black"
        );

        Vehicle sharedVehicle = vehicle.enableHistorySharing();

        assertThat(sharedVehicle.historySharingEnabled()).isTrue();
        assertThat(sharedVehicle.historyPublicId()).isNotNull();
        assertThat(sharedVehicle.enableHistorySharing()).isSameAs(sharedVehicle);

        Vehicle privateVehicle = sharedVehicle.disableHistorySharing();

        assertThat(privateVehicle.historySharingEnabled()).isFalse();
        assertThat(privateVehicle.historyPublicId()).isNull();
        assertThat(privateVehicle.enableHistorySharing().historyPublicId())
                .isNotEqualTo(sharedVehicle.historyPublicId());
    }
}
