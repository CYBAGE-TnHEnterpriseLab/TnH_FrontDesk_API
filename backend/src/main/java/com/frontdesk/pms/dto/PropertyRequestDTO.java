package com.frontdesk.pms.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PropertyRequestDTO {

    @NotBlank(message = "Property name is required")
    private String propertyName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Contact is required")
    private String contact;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotBlank(message = "Night audit time is required")
    private String nightAuditTime;

    @NotBlank(message = "Check-in time is required")
    private String checkinTime;

    @NotBlank(message = "Check-out time is required")
    private String checkoutTime;
}