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

    public static final class Users {

        public static final String NOT_FOUND =
                "User not found.";

        public static final String ALREADY_REGISTERED =
                "User already registered.";

        private Users() {
        }
    }

    public static final class Auth {

        public static final String INVALID_CREDENTIALS =
                "Invalid credentials.";

        public static final String INVALID_REFRESH_TOKEN =
                "Invalid refresh token.";

        private Auth() {
        }
    }
}