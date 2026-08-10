package Policy_Management.Policy.exception;

import Policy_Management.Policy.dto.PolicyDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<Object> response = handler.handleNotFound(new PolicyNotFoundException(9L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("9"));
    }

    @Test
    void handleDuplicatePolicy_returns409() {
        PolicyDto dto = new PolicyDto();
        dto.setPolicyCode("ABC");

        ResponseEntity<Object> response = handler.handleDuplicatePolicy(new DuplicatePolicyException(dto));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleValidationException_returns400() {
        PolicyValidationException ex = new PolicyValidationException(Map.of("policyName", "required"));

        ResponseEntity<Object> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("errors"));
    }

    @Test
    void handleMethodArgumentNotValid_returnsFieldErrors() throws Exception {
        Method method = DummyController.class.getDeclaredMethod("dummy", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dummy");
        bindingResult.addError(new FieldError("dummy", "fieldA", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("must not be blank", body.get("fieldA"));
    }

    static class DummyController {
        public void dummy(String value) {
        }
    }
}
