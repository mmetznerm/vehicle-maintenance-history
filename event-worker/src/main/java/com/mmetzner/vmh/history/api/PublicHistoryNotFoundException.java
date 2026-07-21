package com.mmetzner.vmh.history.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PublicHistoryNotFoundException extends RuntimeException {
    public PublicHistoryNotFoundException() {
        super("Public vehicle history was not found");
    }
}
