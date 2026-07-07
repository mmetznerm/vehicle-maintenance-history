package com.mmetzner.vmh.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode code;

    protected ApplicationException(
            HttpStatus status,
            ApiErrorCode code,
            String message
    ) {
        super(message);
        this.status = status;
        this.code = code;
    }

}