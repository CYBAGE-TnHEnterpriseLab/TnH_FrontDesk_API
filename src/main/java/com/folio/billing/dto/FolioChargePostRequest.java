package com.folio.billing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolioChargePostRequest {

    @JsonAlias("confirmationNo")
    @NotBlank(message = "confirmationNumber is required")
    private String confirmationNumber;
    private String folioId;
    private String folioName;
    private String guestName;
    private String roomNo;
    private String transactionType;
    private String category;
    private String description;
    private BigDecimal amount;
    private BigDecimal charges;
    private BigDecimal credit;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate postingDate;
    private String userId;
    @Valid
    private Transaction transaction;

    public FolioChargePostRequest() {
    }

    // Backward-compatible constructor for the previous flat request contract.
    public FolioChargePostRequest(String confirmationNumber, String roomNo, String guestName, String category,
                                  String description, BigDecimal amount, LocalDate postingDate, String userId) {
        this.confirmationNumber = confirmationNumber;
        this.roomNo = roomNo;
        this.guestName = guestName;
        this.transactionType = "CHARGE";
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.charges = amount;
        this.credit = BigDecimal.ZERO;
        this.postingDate = postingDate;
        this.userId = userId;
    }

    public String confirmationNumber() { return confirmationNumber; }
    public String folioId() { return folioId; }
    public String folioName() { return folioName; }
    public String guestName() { return guestName; }
    public String roomNo() { return roomNo; }
    public Transaction transaction() {
        if (transaction != null) {
            return transaction;
        }
        return new Transaction(transactionType(), category(), description(), amount(), charges(), credit(), postingDate(), userId());
    }
    public String transactionType() { return firstNonBlank(transactionType, transaction == null ? null : transaction.transactionType(), "CHARGE"); }
    public String category() { return firstNonBlank(category, transaction == null ? null : transaction.category(), ""); }
    public String description() { return firstNonBlank(description, transaction == null ? null : transaction.description(), ""); }
    public BigDecimal amount() { return amount != null ? amount : transaction == null ? null : transaction.amount(); }
    public BigDecimal charges() { return charges != null ? charges : transaction == null ? amount() : transaction.charges(); }
    public BigDecimal credit() { return credit != null ? credit : transaction == null ? BigDecimal.ZERO : transaction.credit(); }
    public LocalDate postingDate() { return postingDate != null ? postingDate : transaction == null ? null : transaction.postingDate(); }
    public String userId() { return firstNonBlank(userId, transaction == null ? null : transaction.userId(), ""); }

    @JsonAlias("confirmationNo")
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }
    public void setFolioId(String folioId) { this.folioId = folioId; }
    public void setFolioName(String folioName) { this.folioName = folioName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCharges(BigDecimal charges) { this.charges = charges; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return fallback;
    }

    public static class Transaction {
        private String transactionType;
        private String category;
        private String description;
        private BigDecimal amount;
        private BigDecimal charges;
        private BigDecimal credit;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate postingDate;
        private String userId;

        public Transaction() {
        }

        public Transaction(String transactionType, String category, String description, BigDecimal amount, BigDecimal charges,
                           BigDecimal credit, LocalDate postingDate, String userId) {
            this.transactionType = transactionType;
            this.category = category;
            this.description = description;
            this.amount = amount;
            this.charges = charges;
            this.credit = credit;
            this.postingDate = postingDate;
            this.userId = userId;
        }

        public String transactionType() { return transactionType; }
        public String category() { return category; }
        public String description() { return description; }
        public BigDecimal amount() { return amount; }
        public BigDecimal charges() { return charges; }
        public BigDecimal credit() { return credit; }
        public LocalDate postingDate() { return postingDate; }
        public String userId() { return userId; }

        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public void setCategory(String category) { this.category = category; }
        public void setDescription(String description) { this.description = description; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public void setCharges(BigDecimal charges) { this.charges = charges; }
        public void setCredit(BigDecimal credit) { this.credit = credit; }
        public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
