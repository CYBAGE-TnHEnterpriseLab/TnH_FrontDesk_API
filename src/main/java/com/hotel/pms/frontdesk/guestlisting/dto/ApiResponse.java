package com.hotel.pms.frontdesk.guestlisting.dto;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiResponse<T> {
    boolean success;
    String message;
    T data;
    Object errors;
    OffsetDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errors(null)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(String message, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .errors(errors)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
