package com.pms.property.auth.service;

import com.pms.property.auth.dto.LoginRequest;
import com.pms.property.auth.dto.LoginResponse;
import com.pms.property.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final String configuredUsername;
    private final String configuredPassword;

    public AuthService(
        @Value("${app.auth.username:admin}") String configuredUsername,
        @Value("${app.auth.password:admin123}") String configuredPassword
    ) {
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    public LoginResponse login(LoginRequest request) {
        boolean validPassword = configuredPassword.equals(request.password());
        if (!validPassword) {
            throw new BadRequestException("Invalid password");
        }
        return new LoginResponse(configuredUsername, "NONE", "", 0);
    }
}

