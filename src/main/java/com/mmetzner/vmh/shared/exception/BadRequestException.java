package com.mmetzner.vmh.shared.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApplicationException {

    public BadRequestException(ApiErrorCode code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}