package com.mmetzner.vmh.shared.common;

public final class ApiMessages {

    private ApiMessages() {
    }

    public static final class Common {

        public static final String REQUEST_VALIDATION_FAILED =
                "Request validation failed.";

        public static final String MALFORMED_REQUEST_BODY =
                "Malformed request body.";

        public static final String DATA_INTEGRITY_CONFLICT =
                "The request conflicts with existing data.";

        public static final String UNEXPECTED_SERVER_ERROR =
                "Unexpected server error.";

        private Common() {
        }
    }
}