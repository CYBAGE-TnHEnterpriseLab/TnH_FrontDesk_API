package com.frontdesk.pms.dto;

import com.frontdesk.common.enums.PropertyStatus;
import com.frontdesk.pms.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class PropertyRequestDTO {
    @NotBlank(groups = OnCreate.class)
    @Size(max = 100)
    private String name;

    @NotBlank(groups = OnCreate.class)
    @Email
    private String email;

    @NotBlank(groups = OnCreate.class)
    private String address;

    @NotBlank(groups = OnCreate.class)
    private String contactName;

    @NotBlank(groups = OnCreate.class)
    @Pattern(regexp = "^[0-9]{10}$")
    private String contactNumber;

    @NotBlank(groups = OnCreate.class)
    private String timeZone;

    @NotNull(groups = OnCreate.class)
    private LocalTime checkInTime;

    @NotNull(groups = OnCreate.class)
    private LocalTime checkOutTime;

    @NotNull(groups = OnCreate.class)
    private LocalTime nightAuditTime;

    /**
     * If not provided on create, service defaults to DRAFT.
     */
    private PropertyStatus status;
}
