package com.folio.billing.client.impl;

import com.folio.billing.client.BillingDataClient;
import com.folio.billing.dto.BillingTotals;
import com.folio.billing.dto.FolioTransactionRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class MockBillingDataClient implements BillingDataClient {

    private final Map<String, BillingTotals> totalsByConfirmation = Map.of(
            "CONF-1001", new BillingTotals(new BigDecimal("825.50"), new BigDecimal("500.00")),
            "CONF-2002", new BillingTotals(new BigDecimal("420.00"), new BigDecimal("420.00"))
    );

    private final Map<String, List<FolioTransactionRow>> transactionsByConfirmation = Map.of(
            "CONF-1001",
            List.of(
                    new FolioTransactionRow(
                            LocalDate.of(2026, 8, 1),
                            "TXN-10001",
                            "Charge",
                            "Room",
                            "Deluxe room charge",
                            new BigDecimal("300.00"),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "frontdesk01",
                            LocalDateTime.of(2026, 8, 1, 10, 0),
                            null,
                            null
                    ),
                    new FolioTransactionRow(
                            LocalDate.of(2026, 8, 2),
                            "TXN-10002",
                            "Charge",
                            "Food",
                            "Dinner buffet",
                            new BigDecimal("75.50"),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "restaurant02",
                            LocalDateTime.of(2026, 8, 2, 20, 15),
                            null,
                            null
                    ),
                    new FolioTransactionRow(
                            LocalDate.of(2026, 8, 3),
                            "TXN-10003",
                            "Payment",
                            "Card",
                            "Visa payment",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            new BigDecimal("500.00"),
                            "cashier01",
                            LocalDateTime.of(2026, 8, 3, 9, 5),
                            null,
                            null
                    )
            ),
            "CONF-2002",
            List.of(
                    new FolioTransactionRow(
                            LocalDate.of(2026, 8, 10),
                            "TXN-20001",
                            "Charge",
                            "Room",
                            "Suite room charge",
                            new BigDecimal("420.00"),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "frontdesk03",
                            LocalDateTime.of(2026, 8, 10, 12, 0),
                            null,
                            null
                    ),
                    new FolioTransactionRow(
                            LocalDate.of(2026, 8, 11),
                            "TXN-20002",
                            "Payment",
                            "Card",
                            "Mastercard payment",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            new BigDecimal("420.00"),
                            "cashier03",
                            LocalDateTime.of(2026, 8, 11, 8, 40),
                            null,
                            null
                    )
            )
    );

    @Override
    public BillingTotals getTotals(String confirmationNo) {
        return totalsByConfirmation.getOrDefault(confirmationNo, new BillingTotals(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    public List<FolioTransactionRow> getTransactions(String confirmationNo) {
        return transactionsByConfirmation.getOrDefault(confirmationNo, List.of());
    }
}
