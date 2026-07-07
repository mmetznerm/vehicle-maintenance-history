package com.mmetzner.vmh.maintenance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMaintenanceRequest(

        @NotNull
        LocalDate maintenanceDate,

        @NotNull
        @PositiveOrZero
        Integer odometer,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal cost
) {
}