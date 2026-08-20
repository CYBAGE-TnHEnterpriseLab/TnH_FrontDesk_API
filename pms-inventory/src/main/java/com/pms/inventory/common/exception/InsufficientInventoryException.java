package com.pms.inventory.common.exception;

public class InsufficientInventoryException extends InventoryException {

    public InsufficientInventoryException(String message) {
        super(message);
    }
}

