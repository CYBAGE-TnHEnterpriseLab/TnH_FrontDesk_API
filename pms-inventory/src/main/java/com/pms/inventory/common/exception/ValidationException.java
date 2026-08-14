package com.pms.inventory.common.exception;

public class ValidationException extends BadRequestException {

    private final String code;
    private final String fieldPath;

    public ValidationException(String code, String fieldPath, String message) {
        super(message);
        this.code = code;
        this.fieldPath = fieldPath;
    }

    public String getCode() {
        return code;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}

