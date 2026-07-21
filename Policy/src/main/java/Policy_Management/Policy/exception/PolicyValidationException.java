package Policy_Management.Policy.exception;

import java.util.Map;

public class PolicyValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public PolicyValidationException(Map<String, String> errors) {
        super("Policy validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
