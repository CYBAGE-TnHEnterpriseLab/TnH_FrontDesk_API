package Policy_Management.Policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(PolicyNotFoundException ex) {
        LOGGER.warn("PolicyNotFoundException: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicatePolicyException.class)
    public ResponseEntity<Object> handleDuplicatePolicy(DuplicatePolicyException ex) {
        LOGGER.warn("DuplicatePolicyException: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PolicyValidationException.class)
    public ResponseEntity<Object> handleValidationException(PolicyValidationException ex) {
        LOGGER.warn("PolicyValidationException: {}", ex.getErrors(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("errors", ex.getErrors());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        LOGGER.warn("MethodArgumentNotValidException: {}", errors, ex);
        return new ResponseEntity<>(errors, status);
    }
}
