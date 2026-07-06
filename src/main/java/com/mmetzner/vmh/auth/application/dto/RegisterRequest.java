package com.mmetzner.vmh.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 160)
        String fullName,

        @NotBlank
        @Size(max = 180)
        String emailOrPhone,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}