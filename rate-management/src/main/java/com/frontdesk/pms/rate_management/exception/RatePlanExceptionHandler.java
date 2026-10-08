package com.frontdesk.pms.rate_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RatePlanExceptionHandler {

    private static final String SUCCESS = "success";
    private static final String DATA = "data";
    private static final String MESSAGE = "message";

    private Map<String, Object> errorBody(String message) {
        // Map.of rejects null values, which turned every handled 400 into an unhandled 500.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(SUCCESS, false);
        body.put(DATA, null);
        body.put(MESSAGE, message);
        return body;
    }

    @ExceptionHandler(InvalidRatePlanException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRatePlan(InvalidRatePlanException ex) {
        return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(RatePlanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRatePlanNotFound(RatePlanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePropertyNotFound(PropertyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(errorBody(message));
    }
}