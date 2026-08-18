package com.pms.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInGuestUpdateRequestDto {

    @NotBlank(message = "personalEmail is required")
    @Email(message = "personalEmail must be a valid email")
    private String personalEmail;

    @NotBlank(message = "officialEmail is required")
    @Email(message = "officialEmail must be a valid email")
    private String officialEmail;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "phoneNumber is invalid")
    private String phoneNumber;

    @NotBlank(message = "mobileNumber is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "mobileNumber is invalid")
    private String mobileNumber;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "country is required")
    private String country;

    @NotBlank(message = "zipCode is required")
    private String zipCode;
}
