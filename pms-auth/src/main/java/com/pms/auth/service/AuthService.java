package com.pms.auth.service;

import com.pms.auth.dto.AuthResponse;
import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.LogoutRequest;
import com.pms.auth.dto.RefreshTokenRequest;
import com.pms.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshAccessToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}

