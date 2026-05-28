package com.frontdesk.pms.rate_management.controller;

import com.frontdesk.pms.rate_management.exception.InvalidRatePlanException;
import com.frontdesk.pms.rate_management.exception.PropertyNotFoundException;
import com.frontdesk.pms.rate_management.exception.RatePlanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RatePlanExceptionHandler {

    @ExceptionHandler(InvalidRatePlanException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRatePlan(InvalidRatePlanException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RatePlanNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRatePlanNotFound(RatePlanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePropertyNotFound(PropertyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
