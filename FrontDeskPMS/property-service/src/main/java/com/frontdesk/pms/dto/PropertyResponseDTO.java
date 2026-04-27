package com.frontdesk.pms.dto;

import com.frontdesk.common.enums.PropertyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class PropertyResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String address;

    private String contactName;
    private String contactNumber;

    private String timeZone;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private LocalTime nightAuditTime;

    private PropertyStatus status;
}
