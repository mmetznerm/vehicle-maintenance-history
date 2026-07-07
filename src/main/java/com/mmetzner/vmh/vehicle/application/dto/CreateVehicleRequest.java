package com.mmetzner.vmh.vehicle.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(

        @NotBlank
        @Size(max = 10)
        String plate,

        @NotBlank
        @Size(max = 80)
        String brand,

        @NotBlank
        @Size(max = 80)
        String model,

        @NotNull
        @Min(1886)
        @Max(2100)
        Integer manufactureYear,

        @Size(max = 40)
        String color
) {
}