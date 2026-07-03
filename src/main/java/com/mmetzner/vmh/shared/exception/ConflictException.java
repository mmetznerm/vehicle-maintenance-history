package com.mmetzner.vmh.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {

    public ConflictException(ApiErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}