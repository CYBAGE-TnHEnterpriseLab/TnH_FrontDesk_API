package com.frontdesk.pms.account.service;

import java.util.UUID;

public interface TransactionReferenceService {
    boolean hasTransactionsForAccount(UUID propertyId, UUID chartOfAccountId);
}
