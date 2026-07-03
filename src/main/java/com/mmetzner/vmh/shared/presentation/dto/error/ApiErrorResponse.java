package com.mmetzner.vmh.shared.presentation.dto.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors
) {

    public static ApiErrorResponse withoutFieldErrors(
            int status,
            String error,
            String code,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                code,
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String error,
            String code,
            String message,
            String path,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                code,
                message,
                path,
                List.copyOf(fieldErrors)
        );
    }
}