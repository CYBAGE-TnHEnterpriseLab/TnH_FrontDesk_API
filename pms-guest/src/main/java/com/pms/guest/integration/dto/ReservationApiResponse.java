package com.pms.guest.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the pms-reservation ApiResponse envelope. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
