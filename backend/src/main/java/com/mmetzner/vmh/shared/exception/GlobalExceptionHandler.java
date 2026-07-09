package com.mmetzner.vmh.shared.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.presentation.dto.error.ApiErrorResponse;
import com.mmetzner.vmh.shared.presentation.dto.error.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception.getStatus();

        LOGGER.warn(
                "Application exception handled status={} code={} path={}",
                status.value(),
                exception.getCode(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withoutFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getCode().name(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<FieldErrorResponse> fieldErrors = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorResponse)
                .toList();

        LOGGER.debug(
                "Request validation failed path={} fieldErrorCount={}",
                request.getRequestURI(),
                fieldErrors.size()
        );

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        ApiErrorCode.REQUEST_VALIDATION_FAILED.name(),
                        ApiMessages.Common.REQUEST_VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<FieldErrorResponse> fieldErrors = exception
                .getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        ApiErrorCode.REQUEST_VALIDATION_FAILED.name(),
                        ApiMessages.Common.REQUEST_VALIDATION_FAILED,
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        LOGGER.debug(
                "Malformed request body path={}",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withoutFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        ApiErrorCode.MALFORMED_REQUEST_BODY.name(),
                        ApiMessages.Common.MALFORMED_REQUEST_BODY,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        LOGGER.warn(
                "Data integrity violation path={}",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withoutFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        ApiErrorCode.DATA_INTEGRITY_CONFLICT.name(),
                        ApiMessages.Common.DATA_INTEGRITY_CONFLICT,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        LOGGER.error(
                "Unexpected server error path={}",
                request.getRequestURI(),
                exception
        );

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.withoutFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        ApiErrorCode.UNEXPECTED_SERVER_ERROR.name(),
                        ApiMessages.Common.UNEXPECTED_SERVER_ERROR,
                        request.getRequestURI()
                ));
    }

    private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new FieldErrorResponse(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}