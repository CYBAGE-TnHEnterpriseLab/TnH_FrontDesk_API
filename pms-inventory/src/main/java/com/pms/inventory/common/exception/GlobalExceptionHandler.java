package com.pms.inventory.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
	}

	@ExceptionHandler(InventoryNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			InventoryNotFoundException ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(InsufficientInventoryException.class)
	public ResponseEntity<ErrorResponse> handleInsufficient(
			InsufficientInventoryException ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY", ex.getMessage(), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLocking(
			OptimisticLockingFailureException ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.CONFLICT, "INVENTORY_CONFLICT", "Inventory was modified concurrently. Retry the request.", request);
	}

	@ExceptionHandler(InventoryException.class)
	public ResponseEntity<ErrorResponse> handleInventory(
			InventoryException ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.CONFLICT, "INVENTORY_ERROR", ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(
			Exception ex,
			HttpServletRequest request
	) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected error occurred", request);
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

