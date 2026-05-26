package com.frontdesk.pms.room.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FloorNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFloorNotFound(FloorNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoomTypeNotFound(RoomTypeNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return buildResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoomNotFound(RoomNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PropertyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePropertyNotFound(PropertyNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, String>> buildResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        return new ResponseEntity<>(error, status);
    }
}
