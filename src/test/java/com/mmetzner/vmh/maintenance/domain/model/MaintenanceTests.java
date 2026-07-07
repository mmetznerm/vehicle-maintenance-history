package com.mmetzner.vmh.maintenance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceTests {

    @Test
    void shouldCreateMaintenance() {
        UUID vehicleId = UUID.randomUUID();

        Maintenance maintenance = Maintenance.create(
                vehicleId,
                LocalDate.of(2026, 7, 7),
                35_000,
                " Troca de óleo ",
                new BigDecimal("250.00")
        );

        assertThat(maintenance.id()).isNotNull();
        assertThat(maintenance.vehicleId()).isEqualTo(vehicleId);
        assertThat(maintenance.maintenanceDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(maintenance.odometer()).isEqualTo(35_000);
        assertThat(maintenance.description()).isEqualTo("Troca de óleo");
        assertThat(maintenance.cost()).isEqualByComparingTo("250.00");
    }

    @Test
    void shouldRejectNegativeOdometer() {
        assertThatThrownBy(() -> Maintenance.create(
                UUID.randomUUID(),
                LocalDate.now(),
                -1,
                "Troca de pneus",
                new BigDecimal("800.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("odometer must be greater than or equal to zero");
    }

    @Test
    void shouldRejectBlankDescription() {
        assertThatThrownBy(() -> Maintenance.create(
                UUID.randomUUID(),
                LocalDate.now(),
                10_000,
                " ",
                new BigDecimal("120.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description is required");
    }

    @Test
    void shouldRejectNegativeCost() {
        assertThatThrownBy(() -> Maintenance.create(
                UUID.randomUUID(),
                LocalDate.now(),
                10_000,
                "Alinhamento",
                new BigDecimal("-50.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cost must be greater than or equal to zero");
    }
}