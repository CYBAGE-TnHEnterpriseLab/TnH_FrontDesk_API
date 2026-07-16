package com.frontdesk.pms.rate_management.exception;

public class MasterRoomNotFoundException extends RuntimeException {
    public MasterRoomNotFoundException(Long id) {
        super("Master Room not found with id: " + id);
    }
}