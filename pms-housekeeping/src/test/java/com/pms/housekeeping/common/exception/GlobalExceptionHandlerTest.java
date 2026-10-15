package com.pms.housekeeping.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_shouldReturnBadRequestWithFieldMessages() throws Exception {
        HttpServletRequest request = request("/api/v1/housekeeping/rooms");
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
        bindingResult.addError(new FieldError("payload", "propertyId", "must not be null"));
        bindingResult.addError(new FieldError("payload", "businessDate", "must not be null"));
        Method method = DummyController.class.getDeclaredMethod("handle", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).contains("propertyId: must not be null");
        assertThat(body.message()).contains("businessDate: must not be null");
        assertThat(body.path()).isEqualTo("/api/v1/housekeeping/rooms");
    }

    @Test
    void handleConstraintViolation_shouldReturnValidationError() {
        HttpServletRequest request = request("/api/v1/housekeeping/rooms");
        ConstraintViolationException ex = new ConstraintViolationException("size must be between 1 and 200", Set.of());

        var response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).contains("size must be between 1 and 200");
    }

    @Test
    void handleBadInput_shouldReturnBadRequest() {
        HttpServletRequest request = request("/api/v1/housekeeping/dashboard");

        var messageNotReadable = handler.handleBadInput(new HttpMessageNotReadableException("bad json", new RuntimeException(), null), request);
        assertThat(messageNotReadable.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse messageNotReadableBody = messageNotReadable.getBody();
        assertThat(messageNotReadableBody).isNotNull();
        assertThat(messageNotReadableBody.error()).isEqualTo("BAD_REQUEST");
        assertThat(messageNotReadableBody.message()).isEqualTo("Invalid request input.");

        MethodParameter parameter = methodParameter();
        var typeMismatch = handler.handleBadInput(new MethodArgumentTypeMismatchException("abc", String.class, "propertyId", parameter, new IllegalArgumentException("bad")), request);
        assertThat(typeMismatch.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var missingParam = handler.handleBadInput(new MissingServletRequestParameterException("propertyId", "String"), request);
        assertThat(missingParam.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleMethodNotAllowed_shouldReturn405() {
        HttpServletRequest request = request("/api/v1/housekeeping/rooms");
        var response = handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("POST", List.of("GET")), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void handleMediaType_shouldReturn415() throws Exception {
        HttpServletRequest request = request("/api/v1/housekeeping/rooms");
        var response = handler.handleMediaType(new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of()), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void handleSpecificExceptions_shouldMapCorrectStatusCodes() {
        HttpServletRequest request = request("/api/v1/housekeeping/rooms/101/status");

        var notFound = handler.handleNotFound(new HousekeepingNotFoundException("missing"), request);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody()).isNotNull();
        assertThat(notFound.getBody().error()).isEqualTo("HOUSEKEEPING_NOT_FOUND");

        var optimistic = handler.handleOptimisticLocking(new OptimisticLockingFailureException("concurrent"), request);
        assertThat(optimistic.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(optimistic.getBody()).isNotNull();
        assertThat(optimistic.getBody().error()).isEqualTo("HOUSEKEEPING_CONFLICT");

        var data = handler.handleDataConflict(new DataIntegrityViolationException("duplicate"), request);
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(data.getBody()).isNotNull();
        assertThat(data.getBody().error()).isEqualTo("DATA_CONFLICT");

        var business = handler.handleHousekeeping(new HousekeepingException("business"), request);
        assertThat(business.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(business.getBody()).isNotNull();
        assertThat(business.getBody().error()).isEqualTo("HOUSEKEEPING_ERROR");

        var bad = handler.handleBadRequest(new BadRequestException("bad"), request);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody()).isNotNull();
        assertThat(bad.getBody().error()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleGeneric_shouldReturn500() {
        HttpServletRequest request = request("/api/v1/housekeeping/error");
        var response = handler.handleGeneric(new RuntimeException("boom"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.message()).isEqualTo("Unexpected server error.");
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        void handle(String value) {
        }
    }

    private static MethodParameter methodParameter() {
        try {
            Method method = DummyController.class.getDeclaredMethod("handle", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }
}



