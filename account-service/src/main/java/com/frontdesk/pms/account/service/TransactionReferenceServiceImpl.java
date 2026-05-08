package com.frontdesk.pms.account.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionReferenceServiceImpl implements TransactionReferenceService {

    @Override
    public boolean hasTransactionsForAccount(UUID propertyId, UUID chartOfAccountId) {
        return false;
    }
}
