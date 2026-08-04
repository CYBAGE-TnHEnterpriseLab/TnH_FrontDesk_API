package Policy_Management.Policy.exception;

import java.util.Map;

public class PolicyValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public PolicyValidationException(Map<String, String> errors) {
        super("Policy validation failed");
        this.errors = errors;
    }

    public PolicyValidationException(String string) {
        super("Policy validation failed: " + string);
        this.errors = null;
        //TODO Auto-generated constructor stub
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
