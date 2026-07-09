package com.pms.auth.exception;

import com.pms.auth.common.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(String message) {
        super(message);
    }
}

