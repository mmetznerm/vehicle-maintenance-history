package com.mmetzner.vmh.shared.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(
            ApiErrorCode code,
            String message
    ) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}