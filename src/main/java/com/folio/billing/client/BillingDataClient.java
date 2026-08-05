package com.folio.billing.client;

import com.folio.billing.dto.BillingTotals;
import com.folio.billing.dto.FolioTransactionRow;

import java.util.List;

public interface BillingDataClient {

    BillingTotals getTotals(String confirmationNo);

    List<FolioTransactionRow> getTransactions(String confirmationNo);
}
