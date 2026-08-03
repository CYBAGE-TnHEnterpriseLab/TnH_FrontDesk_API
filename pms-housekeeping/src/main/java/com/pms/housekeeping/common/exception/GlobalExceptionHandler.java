package com.pms.housekeeping.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        log.warn("Validation error [{}]: {}", request.getRequestURI(), message);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadInput(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request input.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Method not allowed [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not allowed.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Unsupported media type [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported content type.", request);
    }

    @ExceptionHandler(HousekeepingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            HousekeepingNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Housekeeping not found [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "HOUSEKEEPING_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        log.warn("Optimistic lock conflict [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "HOUSEKEEPING_CONFLICT",
                "Housekeeping state was modified concurrently. Retry the request.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data conflict [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with existing data.", request);
    }

    @ExceptionHandler(HousekeepingException.class)
    public ResponseEntity<ErrorResponse> handleHousekeeping(
            HousekeepingException ex,
            HttpServletRequest request
    ) {
        log.warn("Housekeeping business error [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "HOUSEKEEPING_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request exception [{}]: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception [{}]", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Unexpected server error.", request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}