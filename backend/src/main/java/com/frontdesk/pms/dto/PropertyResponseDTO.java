package com.frontdesk.pms.dto;

import com.frontdesk.pms.enums.PropertyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PropertyResponseDTO {

    private Long id;
    private String propertyName;
    private String email;
    private String address;
    private String contact;
    private String timezone;
    private String nightAuditTime;
    private String checkinTime;
    private String checkoutTime;
    private PropertyStatus status;
    private LocalDateTime createdAt;
}