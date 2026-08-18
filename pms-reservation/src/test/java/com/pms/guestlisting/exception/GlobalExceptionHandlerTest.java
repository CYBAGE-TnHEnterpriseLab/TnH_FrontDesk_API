package com.pms.guestlisting.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidShouldReturnBadRequestWithFieldErrors() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validatedBody", TestBody.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        TestBody body = new TestBody();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(body, "testBody");
        bindingResult.addError(new FieldError("testBody", "name", "name is required"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<?> response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.toString()).contains("Validation failed");
        assertThat(responseBody.toString()).contains("name");
    }

    @Test
    void handleConstraintViolationShouldReturnBadRequest() {
        ConstraintViolationException exception = new ConstraintViolationException("page must be >= 1", Set.of());

        ResponseEntity<?> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("Validation failed");
    }

    @Test
    void handleBadRequestShouldReturnBadRequest() {
        ResponseEntity<?> response = handler.handleBadRequest(new BadRequestException("bad request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("bad request");
    }

    @Test
    void handleExternalServiceShouldReturnBadGateway() {
        ResponseEntity<?> response = handler.handleExternalService(new ExternalServiceException("external failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("external failure");
    }

    @Test
    void handleMissingRequestParameterShouldReturnBadRequest() throws Exception {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("propertyId", "String");

        ResponseEntity<?> response = handler.handleMissingRequestParameter(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("propertyId is required");
    }

    @Test
    void handleTypeMismatchShouldReturnBadRequest() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "page",
                null,
                new IllegalArgumentException("type mismatch")
        );

        ResponseEntity<?> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("Invalid value for page");
    }

    @Test
    void handleGenericShouldReturnInternalServerError() {
        ResponseEntity<?> response = handler.handleGeneric(new Exception("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toString()).contains("Internal server error");
    }

    @SuppressWarnings("unused")
    private void validatedBody(@Valid TestBody body) {
        // helper method for MethodParameter construction in tests
    }

    private static class TestBody {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

