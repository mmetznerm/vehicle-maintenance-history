package com.mmetzner.vmh.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.presentation.dto.error.ApiErrorResponse;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void mapsApplicationExceptionToConfiguredHttpStatus() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/v1/example");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleApplicationException(
                        new ConflictException(
                                ApiErrorCode.DATA_INTEGRITY_CONFLICT,
                                ApiMessages.Common.DATA_INTEGRITY_CONFLICT
                        ),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().code())
                .isEqualTo(ApiErrorCode.DATA_INTEGRITY_CONFLICT.name());
        assertThat(response.getBody().message())
                .isEqualTo(ApiMessages.Common.DATA_INTEGRITY_CONFLICT);
        assertThat(response.getBody().path()).isEqualTo("/v1/example");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void mapsDatabaseConflictWithoutExposingInternalDetails() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/v1/example");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleDataIntegrityViolation(
                        new DataIntegrityViolationException(
                                "Sensitive database details"
                        ),
                        request
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo(ApiErrorCode.DATA_INTEGRITY_CONFLICT.name());
        assertThat(response.getBody().message())
                .isEqualTo(ApiMessages.Common.DATA_INTEGRITY_CONFLICT);
        assertThat(response.getBody().message())
                .doesNotContain("Sensitive database details");
    }
}