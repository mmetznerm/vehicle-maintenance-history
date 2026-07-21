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
                "Silver"
        );

        assertThat(vehicle.id()).isNotNull();
        assertThat(vehicle.plate()).isEqualTo("ABC1234");
        assertThat(vehicle.brand()).isEqualTo("Honda");
        assertThat(vehicle.model()).isEqualTo("Civic");
        assertThat(vehicle.manufactureYear()).isEqualTo(2020);
        assertThat(vehicle.color()).isEqualTo("Silver");
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
                "Black"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plate is required");
    }
}
