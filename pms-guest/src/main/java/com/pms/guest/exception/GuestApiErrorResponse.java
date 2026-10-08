package com.pms.guest.exception;

public record GuestApiErrorResponse(boolean success, Object data, String message) {
}
