package com.mmetzner.vmh.shared.exception;

import org.springframework.http.HttpStatus;

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

    public HttpStatus getStatus() {
        return status;
    }

    public ApiErrorCode getCode() {
        return code;
    }
}