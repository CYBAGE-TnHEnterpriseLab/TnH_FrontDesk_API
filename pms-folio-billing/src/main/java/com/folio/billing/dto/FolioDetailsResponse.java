package com.folio.billing.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record FolioDetailsResponse(String confirmationNumber, Guest guest, List<Folio> folios, Summary summary) {
 public record Guest(String guestName, String roomNumber) {}
 public record Folio(String folioId, String folioName, boolean isActive, BigDecimal balance, BigDecimal totalCharges, BigDecimal totalCredits, List<Transaction> transactions) {}
 public record Transaction(String transactionId, LocalDate date, String referenceNumber, String transactionType, String category, String description, BigDecimal charges, BigDecimal credit, String userId) {}
 public record Summary(int totalFolios, BigDecimal totalBalance, BigDecimal totalCharges, BigDecimal totalCredits,
					   BigDecimal taxPercent, BigDecimal taxAmount) {}
}
