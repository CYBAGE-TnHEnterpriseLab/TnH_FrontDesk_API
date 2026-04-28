package com.frontdesk.common.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class AmenityDTO {
    private UUID propertyId;
    private String name;
    private boolean active;
}