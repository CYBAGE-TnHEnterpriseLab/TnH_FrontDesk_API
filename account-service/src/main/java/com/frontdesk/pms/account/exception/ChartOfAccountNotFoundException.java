package com.frontdesk.pms.account.exception;

import java.util.UUID;

public class ChartOfAccountNotFoundException extends RuntimeException {
    public ChartOfAccountNotFoundException(UUID accountId) {
        super("Chart of account not found: " + accountId);
    }
}
