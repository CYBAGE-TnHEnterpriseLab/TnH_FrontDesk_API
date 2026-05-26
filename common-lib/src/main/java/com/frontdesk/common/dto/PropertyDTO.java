package com.frontdesk.common.dto;

import lombok.Data;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class PropertyDTO {
    private UUID id;
    private String name;
    private String email;
    private String address;
    private String contactName;
    private String contactNumber;
    private String timeZone;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
}