package com.pms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordVerificationRequest {

    @NotBlank(message = "Password is required")
    private String password;
}