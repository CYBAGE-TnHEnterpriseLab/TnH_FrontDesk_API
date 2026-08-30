package com.pms.property.auth.controller;

import com.pms.property.auth.dto.LoginRequest;
import com.pms.property.auth.dto.LoginResponse;
import com.pms.property.auth.service.AuthService;
import com.pms.property.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login successful"));
    }
}

