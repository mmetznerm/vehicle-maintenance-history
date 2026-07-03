package com.mmetzner.vmh.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(ApiErrorCode code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}