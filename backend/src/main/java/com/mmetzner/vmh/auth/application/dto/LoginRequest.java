package com.mmetzner.vmh.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = 180)
        String emailOrPhone,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}