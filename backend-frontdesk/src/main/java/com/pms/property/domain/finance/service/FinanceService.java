package com.pms.property.domain.finance.service;

import com.pms.property.domain.finance.dto.ChartOfAccountRequest;
import com.pms.property.domain.finance.dto.ChartOfAccountResponse;
import com.pms.property.domain.finance.dto.FinanceSummaryResponse;
import java.util.List;

public interface FinanceService {

    FinanceSummaryResponse getSummaryByPropertyId(String propertyId);

    List<ChartOfAccountResponse> listAccountsByPropertyId(String propertyId);

    ChartOfAccountResponse getAccountById(String propertyId, Long accountId);

    ChartOfAccountResponse createAccount(String propertyId, ChartOfAccountRequest request);

    ChartOfAccountResponse updateAccount(String propertyId, Long accountId, ChartOfAccountRequest request);

    void deleteAccount(String propertyId, Long accountId);
}


