package com.folio.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolioChargePostResponse(
        String confirmationNumber,
        String folioId,
        String folioName,
        Transaction transaction,
        FolioSummary folioSummary,
        CheckoutSummary summary,
        String referenceNumber,
        String transactionType,
        String category,
        String description,
        BigDecimal amount,
        BigDecimal totalAmount,
        LocalDate postingDate,
        BigDecimal totalCharges,
        BigDecimal totalPayment,
        BigDecimal balance
) {
    public record Transaction(String transactionId, String referenceNumber, String transactionType, String category,
                              String description, BigDecimal amount, BigDecimal charges, BigDecimal credit,
                              LocalDate postingDate, String userId) {}
    public record FolioSummary(BigDecimal totalCharges, BigDecimal totalCredits, BigDecimal balance) {}
    public record CheckoutSummary(BigDecimal totalBalance, boolean canCheckout) {}

    public FolioChargePostResponse(String confirmationNumber, String referenceNumber, String transactionType,
                                                                   String category, String description, BigDecimal amount,
                                                                   BigDecimal totalAmount, LocalDate postingDate,
                                   BigDecimal totalCharges, BigDecimal totalPayment, BigDecimal balance) {
        this(confirmationNumber, null, null, null, null, null, referenceNumber, transactionType, category, description,
                                amount, totalAmount, postingDate, totalCharges, totalPayment, balance);
    }
}

