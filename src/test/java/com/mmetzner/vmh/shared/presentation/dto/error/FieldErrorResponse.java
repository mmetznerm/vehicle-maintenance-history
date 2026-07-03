package com.mmetzner.vmh.shared.presentation.dto.error;

public record FieldErrorResponse(
        String field,
        String message
) {
}