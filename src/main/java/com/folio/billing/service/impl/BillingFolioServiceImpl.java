package com.folio.billing.service.impl;

import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.client.PropertyTaxRuleClient;
import com.folio.billing.dto.BillingDetailsResponse;
import com.folio.billing.dto.BillingTotals;
import com.folio.billing.dto.ChargeAdjustmentType;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.FolioChargeAdjustmentRequest;
import com.folio.billing.dto.FolioChargeAdjustmentResponse;
import com.folio.billing.dto.FolioChargePostRequest;
import com.folio.billing.dto.FolioChargePostResponse;
import com.folio.billing.dto.FolioDocumentAuditEntry;
import com.folio.billing.dto.FolioDocumentContent;
import com.folio.billing.dto.FolioDocumentGenerateRequest;
import com.folio.billing.dto.FolioDocumentGenerateResponse;
import com.folio.billing.dto.FolioDocumentType;
import com.folio.billing.dto.FolioDashboardResponse;
import com.folio.billing.dto.FolioPaymentAllocationLineResult;
import com.folio.billing.dto.FolioPaymentAllocationRequest;
import com.folio.billing.dto.FolioPaymentAllocationResponse;
import com.folio.billing.dto.FolioTransactionRow;
import com.folio.billing.dto.GuestDetail;
import com.folio.billing.dto.PaymentAllocationHistoryEntry;
import com.folio.billing.dto.PaymentAllocationTargetRequest;
import com.folio.billing.dto.ReservationSummary;
import com.folio.billing.dto.PropertyTaxRule;
import com.folio.billing.dto.TaxDetail;
import com.folio.billing.entity.FolioTaxSnapshot;
import com.folio.billing.repository.FolioTaxSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folio.billing.service.BillingFolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class BillingFolioServiceImpl implements BillingFolioService {

    private static final BillingTotals ZERO_TOTALS = new BillingTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    private static final String TRANSACTION_TYPE_CHARGE = "Charge";
    private static final String TRANSACTION_TYPE_CHARGE_ADJUSTMENT = "ChargeAdjustment";
    private static final String TRANSACTION_TYPE_PAYMENT = "Payment";
    private static final String DOCUMENT_CONTENT_TYPE = "text/html;charset=UTF-8";

    private final ReservationServiceClient reservationServiceClient;
    private final PropertyTaxRuleClient propertyTaxRuleClient;
    private final FolioTaxSnapshotRepository taxSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, FolioState> foliosByConfirmationNo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<FolioTransactionRow>> postedTransactionsByConfirmationNo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<PaymentAllocationHistoryEntry>> allocationHistoryByConfirmationNo = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FolioDocumentContent> documentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<FolioDocumentAuditEntry>> documentAuditHistoryByConfirmationNo = new ConcurrentHashMap<>();
    private final AtomicLong chargeReferenceSequence = new AtomicLong(100000L);
    private final AtomicLong adjustmentReferenceSequence = new AtomicLong(200000L);
    private final AtomicLong paymentTransactionReferenceSequence = new AtomicLong(300000L);
    private final AtomicLong paymentReferenceSequence = new AtomicLong(400000L);
    private final AtomicLong documentSequence = new AtomicLong(500000L);

    public BillingFolioServiceImpl(
            ReservationServiceClient reservationServiceClient,
            PropertyTaxRuleClient propertyTaxRuleClient,
            FolioTaxSnapshotRepository taxSnapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.reservationServiceClient = reservationServiceClient;
        this.propertyTaxRuleClient = propertyTaxRuleClient;
        this.taxSnapshotRepository = taxSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<FolioBillingRow> getFolioBilling(FolioBillingFilter filter) {
        List<FolioBillingRow> folioRows = reservationServiceClient.searchFolioBilling(filter);

        // Folio entries are created/maintained automatically whenever reservations are listed.
        folioRows.forEach(this::autoCreateOrRefreshFolio);

        return folioRows;
    }

    @Override
    public BillingDetailsResponse getBillingDetails(String confirmationNo, String roomNo, String guestName) {
        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNo, roomNo, guestName)
                .orElseGet(() -> emptySummary(guestName, confirmationNo, roomNo));

        String resolvedConfirmationNo = firstNonBlank(summary.confirmationNo(), normalize(confirmationNo));
        String resolvedRoomNo = firstNonBlank(summary.roomNo(), normalize(roomNo));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                resolvedConfirmationNo,
                summary.guestName(),
                resolvedRoomNo,
                null
        );

        String responseConfirmationNo = firstNonBlank(summary.confirmationNo(), resolvedConfirmationNo);

        return new BillingDetailsResponse(
                balanceSnapshot.totalCharges(),
                balanceSnapshot.totalPayment(),
                balanceSnapshot.balance(),
                defaultString(summary.guestName()),
                defaultString(summary.guest1()),
                defaultString(summary.guest2()),
                defaultString(responseConfirmationNo),
                summary.adults(),
                summary.children(),
                defaultString(summary.company()),
                defaultString(summary.bookingSource()),
                defaultString(summary.ratePlan()),
                defaultString(summary.reservationStatus()),
                defaultString(summary.folioStatus()),
                defaultString(resolvedRoomNo),
                defaultString(summary.roomType()),
                summary.checkInDate(),
                summary.checkOutDate(),
                summary.nights(),
                defaultString(summary.comments())
        );
    }

    @Override
    public FolioDashboardResponse getFolioDashboard(String confirmationNo) {
        String resolvedConfirmationNo = normalize(confirmationNo);

        if (!hasText(resolvedConfirmationNo)) {
            resolvedConfirmationNo = reservationServiceClient.findDefaultConfirmationNo().orElse("");
        }

        if (!hasText(resolvedConfirmationNo)) {
            return new FolioDashboardResponse(List.of(), List.of());
        }

        List<FolioTransactionRow> transactions = getMergedTransactions(resolvedConfirmationNo, null);

        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(resolvedConfirmationNo, null, null)
            .orElse(emptySummary("", resolvedConfirmationNo, ""));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                resolvedConfirmationNo,
                summary.guestName(),
                summary.roomNo(),
                transactions
        );

        List<GuestDetail> rawGuestDetails = Optional
                .ofNullable(reservationServiceClient.getGuestDetails(resolvedConfirmationNo))
                .orElse(List.of());
        List<GuestDetail> guestDetails = applyDueToGuestProfiles(rawGuestDetails, balanceSnapshot.balance());

        if (guestDetails.isEmpty() && hasText(summary.guestName())) {
            guestDetails = List.of(new GuestDetail(
                    summary.guestName(),
                    0,
                    "",
                    "",
                    "",
                    balanceSnapshot.balance()
            ));
        }

        return new FolioDashboardResponse(transactions, guestDetails);
    }

    @Override
    public FolioChargePostResponse addCharge(FolioChargePostRequest request) {
        String confirmationNo = normalize(request.confirmationNo());
        if (!hasText(confirmationNo)) {
            throw badRequest("confirmationNo is required");
        }

        BigDecimal amount = scaleMoney(safeAmount(request.amount()));
        validateChargeAmounts(amount, BigDecimal.ZERO);

        String category = firstNonBlank(request.category(), "ANCILLARY");
        List<TaxDetail> taxDetails = calculateTaxes(amount, category, request.postingDate() != null ? request.postingDate() : LocalDate.now());
        BigDecimal tax = totalTax(taxDetails);
        String description = firstNonBlank(request.description(), category + " charge");
        LocalDate postingDate = request.postingDate() != null ? request.postingDate() : LocalDate.now();
        LocalDateTime postedAt = LocalDateTime.now();
        String userId = firstNonBlank(request.userId(), "frontdesk-agent");
        String referenceNumber = generateChargeReference();

        FolioTransactionRow chargeTransaction = new FolioTransactionRow(
                postingDate,
                referenceNumber,
                TRANSACTION_TYPE_CHARGE,
                category,
                description,
                amount,
                tax,
                BigDecimal.ZERO,
                userId,
                postedAt,
                null,
                null,
                taxDetails
        );

        appendPostedTransaction(confirmationNo, chargeTransaction);

        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNo, request.roomNo(), request.guestName())
                .orElseGet(() -> emptySummary(request.guestName(), confirmationNo, request.roomNo()));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                confirmationNo,
                firstNonBlank(summary.guestName(), request.guestName()),
                firstNonBlank(summary.roomNo(), request.roomNo()),
                null
        );

        return new FolioChargePostResponse(
                confirmationNo,
                referenceNumber,
                TRANSACTION_TYPE_CHARGE,
                category,
                description,
                amount,
                tax,
                taxDetails,
                scaleMoney(amount.add(tax)),
                postingDate,
                balanceSnapshot.totalCharges(),
                balanceSnapshot.totalPayment(),
                balanceSnapshot.balance()
        );
    }

    @Override
    public FolioChargeAdjustmentResponse adjustCharge(FolioChargeAdjustmentRequest request) {
        String confirmationNo = normalize(request.confirmationNo());
        if (!hasText(confirmationNo)) {
            throw badRequest("confirmationNo is required");
        }

        String originalReferenceNumber = normalize(request.originalReferenceNumber());
        if (!hasText(originalReferenceNumber)) {
            throw badRequest("originalReferenceNumber is required");
        }

        if (request.adjustmentType() == null) {
            throw badRequest("adjustmentType is required");
        }

        if (!hasText(request.reason())) {
            throw badRequest("reason is required");
        }

        BigDecimal amount = scaleMoney(safeAmount(request.amount()));
        validateChargeAmounts(amount, BigDecimal.ZERO);

        List<FolioTransactionRow> existingTransactions = getMergedTransactions(confirmationNo, null);

        FolioTransactionRow originalTransaction = existingTransactions.stream()
                .filter(transaction -> originalReferenceNumber.equalsIgnoreCase(defaultString(transaction.referenceNumber())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "original transaction not found"
                ));

        if (!isAdjustableChargeTransaction(originalTransaction)) {
            throw badRequest("only charge transactions can be adjusted");
        }

        List<TaxDetail> taxDetails = calculateAdjustmentTaxes(amount, originalTransaction);
        BigDecimal tax = totalTax(taxDetails);

        BigDecimal signedAmount = request.adjustmentType() == ChargeAdjustmentType.DECREASE
                ? amount.negate()
                : amount;
        BigDecimal signedTax = request.adjustmentType() == ChargeAdjustmentType.DECREASE
                ? tax.negate()
                : tax;
        List<TaxDetail> signedTaxDetails = request.adjustmentType() == ChargeAdjustmentType.DECREASE
                ? taxDetails.stream().map(detail -> new TaxDetail(detail.taxName(), detail.rate(), detail.amount().negate())).toList()
                : taxDetails;

        BigDecimal currentNetChargeForReference = calculateCurrentNetChargeForReference(
                originalReferenceNumber,
                existingTransactions
        );
        BigDecimal adjustmentTotal = chargeComponent(signedAmount, signedTax);

        if (request.adjustmentType() == ChargeAdjustmentType.DECREASE
                && adjustmentTotal.abs().compareTo(currentNetChargeForReference) > 0) {
            throw badRequest("decrease amount exceeds current posted charge value");
        }

        String adjustmentReferenceNumber = generateAdjustmentReference();
        LocalDate postingDate = LocalDate.now();
        LocalDateTime postedAt = LocalDateTime.now();
        String userId = firstNonBlank(request.userId(), "frontdesk-agent");
        String category = firstNonBlank(originalTransaction.category(), "ADJUSTMENT");

        FolioTransactionRow adjustmentTransaction = new FolioTransactionRow(
                postingDate,
                adjustmentReferenceNumber,
                TRANSACTION_TYPE_CHARGE_ADJUSTMENT,
                category,
                "Adjustment for " + originalReferenceNumber + ": " + request.reason().trim(),
                signedAmount,
                signedTax,
                BigDecimal.ZERO,
                userId,
                postedAt,
                originalReferenceNumber,
                request.reason().trim(),
                signedTaxDetails
        );

        appendPostedTransaction(confirmationNo, adjustmentTransaction);

        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNo, null, null)
                .orElseGet(() -> emptySummary("", confirmationNo, ""));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                confirmationNo,
                summary.guestName(),
                summary.roomNo(),
                null
        );

        return new FolioChargeAdjustmentResponse(
                confirmationNo,
                originalReferenceNumber,
                adjustmentReferenceNumber,
                request.adjustmentType(),
                category,
                request.reason().trim(),
                amount,
                tax,
                signedTaxDetails,
                scaleMoney(signedAmount.add(signedTax)),
                postingDate,
                postedAt,
                userId,
                balanceSnapshot.totalCharges(),
                balanceSnapshot.totalPayment(),
                balanceSnapshot.balance()
        );
    }

    @Override
    public FolioPaymentAllocationResponse allocatePayment(FolioPaymentAllocationRequest request) {
        if (request == null) {
            throw badRequest("request is required");
        }

        BigDecimal paymentAmount = scaleMoney(safeAmount(request.paymentAmount()));
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("paymentAmount must be greater than zero");
        }

        List<PaymentAllocationTargetRequest> requestedAllocations = Optional
                .ofNullable(request.allocations())
                .orElse(List.of());

        if (requestedAllocations.isEmpty()) {
            throw badRequest("allocations are required");
        }

        Map<String, BigDecimal> allocationsByConfirmationNo = new LinkedHashMap<>();
        for (PaymentAllocationTargetRequest allocation : requestedAllocations) {
            if (allocation == null) {
                throw badRequest("allocation entry is required");
            }

            String confirmationNo = normalize(allocation.confirmationNo());
            if (!hasText(confirmationNo)) {
                throw badRequest("confirmationNo is required for all allocations");
            }

            BigDecimal allocationAmount = scaleMoney(safeAmount(allocation.amount()));
            if (allocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("allocation amount must be greater than zero");
            }

            allocationsByConfirmationNo.merge(confirmationNo, allocationAmount, BigDecimal::add);
        }

        BigDecimal totalAllocatedAmount = scaleMoney(
                allocationsByConfirmationNo.values().stream()
                        .map(BillingFolioServiceImpl::safeAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        if (totalAllocatedAmount.compareTo(paymentAmount) > 0) {
            throw badRequest("allocated amount exceeds payment amount");
        }

        Map<String, BigDecimal> balancesBeforeAllocation = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> allocationEntry : allocationsByConfirmationNo.entrySet()) {
            String confirmationNo = allocationEntry.getKey();
            BigDecimal requestedAmount = scaleMoney(allocationEntry.getValue());
            BalanceSnapshot snapshot = syncFolioWithLatestBalances(confirmationNo, "", "", null);
            BigDecimal outstandingBalance = scaleMoney(safeAmount(snapshot.balance()));

            if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("folio has no outstanding balance for confirmationNo: " + confirmationNo);
            }

            if (requestedAmount.compareTo(outstandingBalance) > 0) {
                throw badRequest("allocation amount exceeds folio outstanding balance for confirmationNo: " + confirmationNo);
            }

            balancesBeforeAllocation.put(confirmationNo, outstandingBalance);
        }

        String paymentReference = firstNonBlank(request.paymentReference(), generatePaymentReference());
        String paymentMethod = firstNonBlank(request.paymentMethod(), "Card");
        LocalDate allocationDate = request.allocationDate() != null ? request.allocationDate() : LocalDate.now();
        LocalDateTime allocatedAt = LocalDateTime.now();
        String userId = firstNonBlank(request.userId(), "cashier-agent");
        String note = defaultString(request.note());
        BigDecimal unallocatedAmount = scaleMoney(paymentAmount.subtract(totalAllocatedAmount));

        List<FolioPaymentAllocationLineResult> allocationResults = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> allocationEntry : allocationsByConfirmationNo.entrySet()) {
            String confirmationNo = allocationEntry.getKey();
            BigDecimal allocatedAmount = scaleMoney(allocationEntry.getValue());
            BigDecimal balanceBeforeAllocation = balancesBeforeAllocation.getOrDefault(confirmationNo, BigDecimal.ZERO);
            String transactionReferenceNumber = generatePaymentTransactionReference();

            FolioTransactionRow paymentTransaction = new FolioTransactionRow(
                    allocationDate,
                    transactionReferenceNumber,
                    TRANSACTION_TYPE_PAYMENT,
                    paymentMethod,
                    "Payment allocation " + paymentReference,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    allocatedAmount,
                    userId,
                    allocatedAt,
                    paymentReference,
                    note,
                    List.of()
            );

            appendPostedTransaction(confirmationNo, paymentTransaction);

            ReservationSummary summary = reservationServiceClient
                    .getReservationSummary(confirmationNo, null, null)
                    .orElseGet(() -> emptySummary("", confirmationNo, ""));

            BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                    confirmationNo,
                    summary.guestName(),
                    summary.roomNo(),
                    null
            );

            allocationResults.add(new FolioPaymentAllocationLineResult(
                    confirmationNo,
                    transactionReferenceNumber,
                    allocatedAmount,
                    balanceBeforeAllocation,
                    balanceSnapshot.balance()
            ));

            appendAllocationHistory(
                    confirmationNo,
                    new PaymentAllocationHistoryEntry(
                            paymentReference,
                            confirmationNo,
                            paymentAmount,
                            totalAllocatedAmount,
                            allocatedAmount,
                            unallocatedAmount,
                            paymentMethod,
                            allocationDate,
                            allocatedAt,
                            userId,
                            note,
                            balanceSnapshot.balance()
                    )
            );
        }

        return new FolioPaymentAllocationResponse(
                paymentReference,
                paymentAmount,
                totalAllocatedAmount,
                unallocatedAmount,
                paymentMethod,
                allocationDate,
                allocatedAt,
                userId,
                List.copyOf(allocationResults)
        );
    }

    @Override
    public List<PaymentAllocationHistoryEntry> getPaymentAllocationHistory(String confirmationNo, String paymentReference) {
        String normalizedConfirmationNo = normalize(confirmationNo);
        String normalizedPaymentReference = normalize(paymentReference);

        Stream<PaymentAllocationHistoryEntry> historyStream = hasText(normalizedConfirmationNo)
                ? allocationHistoryByConfirmationNo.getOrDefault(normalizedConfirmationNo, List.of()).stream()
                : allocationHistoryByConfirmationNo.values().stream().flatMap(List::stream);

        if (hasText(normalizedPaymentReference)) {
            historyStream = historyStream.filter(historyEntry ->
                    normalizedPaymentReference.equalsIgnoreCase(defaultString(historyEntry.paymentReference()))
            );
        }

        return historyStream
                .sorted(Comparator
                        .comparing(PaymentAllocationHistoryEntry::allocatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PaymentAllocationHistoryEntry::paymentReference, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PaymentAllocationHistoryEntry::confirmationNo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

            @Override
            public FolioDocumentGenerateResponse generateFolioDocument(FolioDocumentGenerateRequest request) {
            if (request == null) {
                throw badRequest("request is required");
            }

            String confirmationNo = normalize(request.confirmationNo());
            if (!hasText(confirmationNo)) {
                throw badRequest("confirmationNo is required");
            }

            if (request.documentType() == null) {
                throw badRequest("documentType is required");
            }

            String generatedBy = firstNonBlank(request.userId(), "frontdesk-agent");
            LocalDateTime generatedAt = LocalDateTime.now();

            ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNo, null, null)
                .orElseGet(() -> emptySummary("", confirmationNo, ""));

            List<FolioTransactionRow> transactions = getMergedTransactions(confirmationNo, null);
            BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                confirmationNo,
                summary.guestName(),
                summary.roomNo(),
                transactions
            );
            DocumentFinancialSnapshot financialSnapshot = buildDocumentFinancialSnapshot(transactions, balanceSnapshot);

            String documentId = generateDocumentId();
            String fileName = buildDocumentFileName(request.documentType(), confirmationNo, documentId);
            String content = buildDocumentHtml(
                documentId,
                confirmationNo,
                request.documentType(),
                generatedBy,
                generatedAt,
                summary,
                transactions,
                financialSnapshot
            );

            FolioDocumentContent documentContent = new FolioDocumentContent(
                documentId,
                confirmationNo,
                request.documentType(),
                fileName,
                DOCUMENT_CONTENT_TYPE,
                content,
                generatedAt,
                generatedBy
            );
            documentsById.put(documentId, documentContent);

            appendDocumentAuditHistory(
                confirmationNo,
                new FolioDocumentAuditEntry(
                    documentId,
                    confirmationNo,
                    request.documentType(),
                    fileName,
                    generatedAt,
                    generatedBy,
                    financialSnapshot.totalChargeAmount(),
                    financialSnapshot.totalTaxAmount(),
                    financialSnapshot.totalPaymentAmount(),
                    financialSnapshot.latestBalance()
                )
            );

            String downloadPath = "/api/v1/billingFolio/documents/" + documentId + "/download";
            String printPath = "/api/v1/billingFolio/documents/" + documentId + "/print";

            return new FolioDocumentGenerateResponse(
                documentId,
                confirmationNo,
                request.documentType(),
                fileName,
                DOCUMENT_CONTENT_TYPE,
                generatedAt,
                generatedBy,
                financialSnapshot.totalChargeAmount(),
                financialSnapshot.totalTaxAmount(),
                financialSnapshot.totalPaymentAmount(),
                financialSnapshot.latestBalance(),
                downloadPath,
                printPath
            );
            }

            @Override
            public FolioDocumentContent getFolioDocument(String documentId) {
            String normalizedDocumentId = normalize(documentId);
            if (!hasText(normalizedDocumentId)) {
                throw badRequest("documentId is required");
            }

            return Optional.ofNullable(documentsById.get(normalizedDocumentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));
            }

            @Override
            public List<FolioDocumentAuditEntry> getFolioDocumentAuditHistory(String confirmationNo, FolioDocumentType documentType) {
            String normalizedConfirmationNo = normalize(confirmationNo);

            Stream<FolioDocumentAuditEntry> historyStream = hasText(normalizedConfirmationNo)
                ? documentAuditHistoryByConfirmationNo.getOrDefault(normalizedConfirmationNo, List.of()).stream()
                : documentAuditHistoryByConfirmationNo.values().stream().flatMap(List::stream);

            if (documentType != null) {
                historyStream = historyStream.filter(entry -> documentType == entry.documentType());
            }

            return historyStream
                .sorted(Comparator
                    .comparing(FolioDocumentAuditEntry::generatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(FolioDocumentAuditEntry::documentId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
            }

    private void autoCreateOrRefreshFolio(FolioBillingRow row) {
        if (row == null || !hasText(row.confirmationNo())) {
            return;
        }

        syncFolioWithLatestBalances(
                row.confirmationNo(),
                row.guest(),
                row.room(),
                null
        );
    }

    private BalanceSnapshot syncFolioWithLatestBalances(
            String confirmationNo,
            String guestName,
            String roomNo,
            List<FolioTransactionRow> knownTransactions
    ) {
        BillingTotals totals = resolveTotals(confirmationNo, knownTransactions);
        BigDecimal totalCharges = safeAmount(totals.totalCharges());
        BigDecimal totalPayment = safeAmount(totals.totalPayment());

        if (!hasText(confirmationNo)) {
            return new BalanceSnapshot(totalCharges, totalPayment, totalCharges.subtract(totalPayment));
        }

        FolioState folioState = upsertFolio(
                confirmationNo,
                guestName,
                roomNo,
                totalCharges,
                totalPayment
        );

        return folioState.snapshot();
    }

    private BillingTotals resolveTotals(String confirmationNo, List<FolioTransactionRow> knownTransactions) {
        if (!hasText(confirmationNo)) {
            return ZERO_TOTALS;
        }

        String normalizedConfirmationNo = normalize(confirmationNo);
        List<FolioTransactionRow> transactions = knownTransactions == null
                ? getMergedTransactions(normalizedConfirmationNo, null)
                : knownTransactions;

        if (!transactions.isEmpty()) {
            return totalsFromTransactions(transactions);
        }

        return ZERO_TOTALS;
    }

    private BillingTotals totalsFromTransactions(List<FolioTransactionRow> transactions) {
        BigDecimal charges = transactions.stream()
                .map(BillingFolioServiceImpl::chargeComponent)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Credits include advance payments and other payment credits posted to folio.
        BigDecimal payments = transactions.stream()
                .map(FolioTransactionRow::credit)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BillingTotals(charges, payments);
    }

    private BigDecimal calculateCurrentNetChargeForReference(
            String originalReferenceNumber,
            List<FolioTransactionRow> transactions
    ) {
        BigDecimal baseCharge = transactions.stream()
                .filter(transaction -> originalReferenceNumber.equalsIgnoreCase(defaultString(transaction.referenceNumber())))
                .findFirst()
                .map(BillingFolioServiceImpl::chargeComponent)
                .orElse(BigDecimal.ZERO);

        BigDecimal adjustments = transactions.stream()
                .filter(transaction -> originalReferenceNumber.equalsIgnoreCase(defaultString(transaction.originalReferenceNumber())))
                .map(BillingFolioServiceImpl::chargeComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return baseCharge.add(adjustments);
    }

    private boolean isAdjustableChargeTransaction(FolioTransactionRow transaction) {
        if (transaction == null) {
            return false;
        }

        BigDecimal chargeValue = chargeComponent(transaction);
        BigDecimal creditValue = safeAmount(transaction.credit());

        return chargeValue.compareTo(BigDecimal.ZERO) > 0
                && creditValue.compareTo(BigDecimal.ZERO) == 0;
    }

    private static BigDecimal chargeComponent(FolioTransactionRow transaction) {
        if (transaction == null) {
            return BigDecimal.ZERO;
        }
        return chargeComponent(transaction.charges(), transaction.tax());
    }

    private static BigDecimal chargeComponent(BigDecimal charges, BigDecimal tax) {
        return safeAmount(charges).add(safeAmount(tax));
    }

    private List<TaxDetail> calculateTaxes(BigDecimal amount, String category, LocalDate postingDate) {
        if ("ROOM".equalsIgnoreCase(defaultString(category).trim())) {
            return List.of();
        }

        String propertyId = resolvePropertyId();
        return propertyTaxRuleClient.getTaxRules(propertyId).stream()
                .filter(rule -> rule.active() && "ACTIVE".equalsIgnoreCase(rule.status()))
                .filter(rule -> "ADD_ON".equalsIgnoreCase(rule.applicableOn()))
                .filter(rule -> rule.effectiveDate() == null || !rule.effectiveDate().isAfter(postingDate))
                .sorted(Comparator.comparing(rule -> Optional.ofNullable(rule.priority()).orElse(Integer.MAX_VALUE)))
                .map(rule -> toTaxDetail(amount, rule))
                .toList();
    }

    private TaxDetail toTaxDetail(BigDecimal amount, PropertyTaxRule rule) {
        if (!"PERCENTAGE".equalsIgnoreCase(rule.type())) {
            throw badRequest("Unsupported tax rule type: " + rule.type());
        }
        if (!"EXCLUSIVE".equalsIgnoreCase(rule.inclExcl())) {
            throw badRequest("Only EXCLUSIVE tax rules are supported for charge posting");
        }
        BigDecimal rate = safeAmount(rule.rate());
        BigDecimal taxAmount = scaleMoney(amount.multiply(rate).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return new TaxDetail(firstNonBlank(rule.taxName(), "Tax"), rate, taxAmount);
    }

    private List<TaxDetail> calculateAdjustmentTaxes(BigDecimal amount, FolioTransactionRow originalTransaction) {
        return Optional.ofNullable(originalTransaction.taxDetails()).orElse(List.of()).stream()
                .map(detail -> new TaxDetail(detail.taxName(), detail.rate(),
                        scaleMoney(amount.multiply(safeAmount(detail.rate())).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))))
                .toList();
    }

    private BigDecimal totalTax(List<TaxDetail> taxDetails) {
        return scaleMoney(taxDetails.stream().map(TaxDetail::amount).map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private String resolvePropertyId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? "" : defaultString(attributes.getRequest().getHeader("X-Property-Id")).trim();
    }

    private List<FolioTransactionRow> getMergedTransactions(String confirmationNo, List<FolioTransactionRow> knownBaseTransactions) {
        if (!hasText(confirmationNo)) {
            return List.of();
        }

        String normalizedConfirmationNo = normalize(confirmationNo);

        List<FolioTransactionRow> baseTransactions = knownBaseTransactions == null ? List.of() : knownBaseTransactions;

        List<FolioTransactionRow> postedTransactions = postedTransactionsByConfirmationNo
                .getOrDefault(normalizedConfirmationNo, List.of());

        if (baseTransactions.isEmpty() && postedTransactions.isEmpty()) {
            return List.of();
        }

        return Stream.concat(baseTransactions.stream(), postedTransactions.stream())
                .sorted(Comparator
                        .comparing(FolioTransactionRow::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FolioTransactionRow::postedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FolioTransactionRow::referenceNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void appendPostedTransaction(String confirmationNo, FolioTransactionRow transaction) {
        String normalizedConfirmationNo = normalize(confirmationNo);

        postedTransactionsByConfirmationNo.compute(normalizedConfirmationNo, (key, existingTransactions) -> {
            List<FolioTransactionRow> mergedTransactions = existingTransactions == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingTransactions);
            mergedTransactions.add(transaction);
            return List.copyOf(mergedTransactions);
        });
        persistTaxSnapshot(normalizedConfirmationNo, transaction);
    }

    private void persistTaxSnapshot(String confirmationNo, FolioTransactionRow transaction) {
        try {
            taxSnapshotRepository.save(new FolioTaxSnapshot(
                    confirmationNo,
                    transaction.referenceNumber(),
                    objectMapper.writeValueAsString(Optional.ofNullable(transaction.taxDetails()).orElse(List.of()))
            ));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to persist folio tax details", ex);
        }
    }

    private void appendAllocationHistory(String confirmationNo, PaymentAllocationHistoryEntry historyEntry) {
        String normalizedConfirmationNo = normalize(confirmationNo);

        allocationHistoryByConfirmationNo.compute(normalizedConfirmationNo, (key, existingEntries) -> {
            List<PaymentAllocationHistoryEntry> mergedEntries = existingEntries == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingEntries);
            mergedEntries.add(historyEntry);
            return List.copyOf(mergedEntries);
        });
    }

    private void appendDocumentAuditHistory(String confirmationNo, FolioDocumentAuditEntry historyEntry) {
        String normalizedConfirmationNo = normalize(confirmationNo);

        documentAuditHistoryByConfirmationNo.compute(normalizedConfirmationNo, (key, existingEntries) -> {
            List<FolioDocumentAuditEntry> mergedEntries = existingEntries == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingEntries);
            mergedEntries.add(historyEntry);
            return List.copyOf(mergedEntries);
        });
    }

    private FolioState upsertFolio(
            String confirmationNo,
            String guestName,
            String roomNo,
            BigDecimal totalCharges,
            BigDecimal totalPayment
    ) {
        String normalizedConfirmationNo = normalize(confirmationNo);
        Instant now = Instant.now();

        return foliosByConfirmationNo.compute(normalizedConfirmationNo, (key, existing) -> {
            String resolvedGuestName = firstNonBlank(guestName, existing == null ? "" : existing.guestName());
            String resolvedRoomNo = firstNonBlank(roomNo, existing == null ? "" : existing.roomNo());
            Instant createdAt = existing == null ? now : existing.createdAt();

            return new FolioState(
                    key,
                    resolvedGuestName,
                    resolvedRoomNo,
                    totalCharges,
                    totalPayment,
                    totalCharges.subtract(totalPayment),
                    createdAt,
                    now
            );
        });
    }

    private List<GuestDetail> applyDueToGuestProfiles(List<GuestDetail> guestDetails, BigDecimal dueAmount) {
        if (guestDetails == null || guestDetails.isEmpty()) {
            return List.of();
        }

        BigDecimal normalizedDue = safeAmount(dueAmount);

        return guestDetails.stream()
                .map(detail -> new GuestDetail(
                        detail.guestName(),
                        detail.guestAge(),
                        detail.guestPhoneNumber(),
                        detail.guestEmailId(),
                        detail.guestAddress(),
                        normalizedDue
                ))
                .toList();
    }

    private ReservationSummary emptySummary(String guestName, String confirmationNo, String roomNo) {
        return new ReservationSummary(
                defaultString(guestName),
                "",
                "",
                defaultString(confirmationNo),
                0,
                0,
                "",
                "",
                "",
                "",
                "",
                defaultString(roomNo),
                "",
                null,
                null,
                0,
                ""
        );
    }

    private DocumentFinancialSnapshot buildDocumentFinancialSnapshot(
            List<FolioTransactionRow> transactions,
            BalanceSnapshot balanceSnapshot
    ) {
        BigDecimal totalChargeAmount = transactions.stream()
                .map(FolioTransactionRow::charges)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTaxAmount = transactions.stream()
                .map(FolioTransactionRow::tax)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaymentAmount = transactions.stream()
                .map(FolioTransactionRow::credit)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal latestBalance = safeAmount(balanceSnapshot.balance());

        return new DocumentFinancialSnapshot(
                scaleMoney(totalChargeAmount),
                scaleMoney(totalTaxAmount),
                scaleMoney(totalPaymentAmount),
                scaleMoney(totalChargeAmount.add(totalTaxAmount)),
                scaleMoney(latestBalance)
        );
    }

    private String buildDocumentHtml(
            String documentId,
            String confirmationNo,
            FolioDocumentType documentType,
            String generatedBy,
            LocalDateTime generatedAt,
            ReservationSummary summary,
            List<FolioTransactionRow> transactions,
            DocumentFinancialSnapshot financialSnapshot
    ) {
        String title = documentType == FolioDocumentType.RECEIPT ? "Folio Payment Receipt" : "Folio Invoice";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html><head><meta charset=\"UTF-8\">")
                .append("<title>").append(escapeHtml(title)).append("</title>")
                .append("<style>")
                .append("body{font-family:Segoe UI,Arial,sans-serif;margin:24px;color:#1e293b;background:#f8fafc;}")
                .append(".sheet{max-width:1100px;margin:0 auto;background:#ffffff;border:1px solid #dbeafe;border-radius:12px;padding:24px;}")
                .append("h1{margin:0 0 6px 0;font-size:28px;color:#0f172a;}")
                .append("h2{font-size:16px;margin:22px 0 10px 0;color:#1d4ed8;border-bottom:1px solid #dbeafe;padding-bottom:4px;}")
                .append(".meta{font-size:13px;color:#334155;margin-bottom:10px;}")
                .append(".grid{display:grid;grid-template-columns:1fr 1fr;gap:10px 24px;font-size:14px;}")
                .append(".grid div{padding:2px 0;}")
                .append("table{width:100%;border-collapse:collapse;margin-top:8px;font-size:13px;}")
                .append("th,td{border:1px solid #cbd5e1;padding:8px 6px;text-align:left;vertical-align:top;}")
                .append("th{background:#eff6ff;color:#1e3a8a;}")
                .append("td.num{text-align:right;font-variant-numeric:tabular-nums;}")
                .append(".summary{margin-top:16px;display:grid;grid-template-columns:1fr 1fr;gap:8px 16px;font-size:14px;}")
                .append(".summary .label{color:#334155;}")
                .append(".summary .value{text-align:right;font-weight:600;font-variant-numeric:tabular-nums;}")
                .append(".actions{margin-bottom:16px;}")
                .append(".actions button{background:#2563eb;color:#fff;border:none;border-radius:6px;padding:8px 14px;cursor:pointer;}")
                .append("@media print {.actions{display:none;} body{background:#fff;margin:0;} .sheet{border:none;border-radius:0;padding:0;}}")
                .append("</style></head><body><div class=\"sheet\">")
                .append("<div class=\"actions\"><button onclick=\"window.print()\">Print Document</button></div>")
                .append("<h1>").append(escapeHtml(title)).append("</h1>")
                .append("<div class=\"meta\">Document ID: ").append(escapeHtml(documentId)).append(" | Confirmation: ")
                .append(escapeHtml(confirmationNo)).append(" | Generated: ").append(escapeHtml(formatDateTime(generatedAt)))
                .append(" | User: ").append(escapeHtml(generatedBy)).append("</div>")
                .append("<h2>Guest And Reservation Details</h2>")
                .append("<div class=\"grid\"> ");

        appendDetailRow(html, "Guest Name", summary.guestName());
        appendDetailRow(html, "Confirmation No", confirmationNo);
        appendDetailRow(html, "Room No", summary.roomNo());
        appendDetailRow(html, "Room Type", summary.roomType());
        appendDetailRow(html, "Check-In Date", formatDate(summary.checkInDate()));
        appendDetailRow(html, "Check-Out Date", formatDate(summary.checkOutDate()));
        appendDetailRow(html, "Nights", Integer.toString(summary.nights()));
        appendDetailRow(html, "Adults", Integer.toString(summary.adults()));
        appendDetailRow(html, "Children", Integer.toString(summary.children()));
        appendDetailRow(html, "Company", summary.company());
        appendDetailRow(html, "Booking Source", summary.bookingSource());
        appendDetailRow(html, "Folio Status", summary.folioStatus());

        html.append("</div>")
                .append("<h2>Charge Tax And Payment Details</h2>")
                .append("<table><thead><tr>")
                .append("<th>Date</th><th>Reference</th><th>Type</th><th>Category</th><th>Description</th>")
                .append("<th>Charge</th><th>Tax</th><th>Payment</th><th>User</th><th>Posted At</th>")
                .append("</tr></thead><tbody>");

        if (transactions.isEmpty()) {
            html.append("<tr><td colspan=\"10\">No folio transactions available.</td></tr>");
        } else {
            for (FolioTransactionRow transaction : transactions) {
                html.append("<tr>")
                        .append("<td>").append(escapeHtml(formatDate(transaction.date()))).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.referenceNumber())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.transactionType())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.category())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.description())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(transaction.charges())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(transaction.tax())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(transaction.credit())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.userId())).append("</td>")
                        .append("<td>").append(escapeHtml(formatDateTime(transaction.postedAt()))).append("</td>")
                        .append("</tr>");
            }
        }

        html.append("</tbody></table>")
                .append("<h2>Tax Breakdown</h2>")
                .append("<table><thead><tr><th>Tax Type</th><th>Rate</th><th>Amount</th></tr></thead><tbody>");

        Map<String, TaxDetail> taxTotals = new LinkedHashMap<>();
        for (FolioTransactionRow transaction : transactions) {
            for (TaxDetail detail : Optional.ofNullable(transaction.taxDetails()).orElse(List.of())) {
                TaxDetail current = taxTotals.get(detail.taxName());
                taxTotals.put(detail.taxName(), new TaxDetail(detail.taxName(), detail.rate(),
                        safeAmount(current == null ? BigDecimal.ZERO : current.amount()).add(safeAmount(detail.amount()))));
            }
        }
        if (taxTotals.isEmpty()) {
            html.append("<tr><td colspan=\"3\">No tax applied.</td></tr>");
        } else {
            for (TaxDetail detail : taxTotals.values()) {
                html.append("<tr><td>").append(escapeHtml(detail.taxName())).append("</td><td class=\"num\">")
                        .append(escapeHtml(detail.rate() == null ? "0" : detail.rate().stripTrailingZeros().toPlainString())).append("%</td><td class=\"num\">")
                        .append(formatMoney(detail.amount())).append("</td></tr>");
            }
        }

        html.append("</tbody></table>")
                .append("<h2>Latest Folio Summary</h2>")
                .append("<div class=\"summary\">")
                .append("<div class=\"label\">Total Charge Amount</div><div class=\"value\">").append(formatMoney(financialSnapshot.totalChargeAmount())).append("</div>")
                .append("<div class=\"label\">Total Tax Amount</div><div class=\"value\">").append(formatMoney(financialSnapshot.totalTaxAmount())).append("</div>")
                .append("<div class=\"label\">Total Charges Including Tax</div><div class=\"value\">").append(formatMoney(financialSnapshot.totalChargesIncludingTax())).append("</div>")
                .append("<div class=\"label\">Total Payment Amount</div><div class=\"value\">").append(formatMoney(financialSnapshot.totalPaymentAmount())).append("</div>")
                .append("<div class=\"label\">Latest Folio Balance</div><div class=\"value\">").append(formatMoney(financialSnapshot.latestBalance())).append("</div>")
                .append("</div>")
                .append("</div></body></html>");

        return html.toString();
    }

    private void appendDetailRow(StringBuilder html, String label, String value) {
        html.append("<div><strong>")
                .append(escapeHtml(label))
                .append(":</strong> ")
                .append(escapeHtml(nonBlankOrNA(value)))
                .append("</div>");
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "N/A" : date.toString();
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "N/A" : dateTime.toString().replace('T', ' ');
    }

    private static String formatMoney(BigDecimal amount) {
        return scaleMoney(amount).toPlainString();
    }

    private static String nonBlankOrNA(String value) {
        return hasText(value) ? value : "N/A";
    }

    private static String escapeHtml(String value) {
        String safe = defaultString(value);
        return safe
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void validateChargeAmounts(BigDecimal amount, BigDecimal tax) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("amount must be greater than zero");
        }
        if (tax.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("tax cannot be negative");
        }
    }

    private String generateChargeReference() {
        return "TXN-CHG-" + chargeReferenceSequence.incrementAndGet();
    }

    private String generateAdjustmentReference() {
        return "TXN-ADJ-" + adjustmentReferenceSequence.incrementAndGet();
    }

    private String generatePaymentTransactionReference() {
        return "TXN-PAY-" + paymentTransactionReferenceSequence.incrementAndGet();
    }

    private String generatePaymentReference() {
        return "PAY-ALLOC-" + paymentReferenceSequence.incrementAndGet();
    }

    private String generateDocumentId() {
        return "DOC-" + documentSequence.incrementAndGet();
    }

    private String buildDocumentFileName(FolioDocumentType documentType, String confirmationNo, String documentId) {
        String prefix = documentType == FolioDocumentType.RECEIPT ? "receipt" : "invoice";
        return prefix + "-" + confirmationNo + "-" + documentId + ".html";
    }

    private static BigDecimal scaleMoney(BigDecimal value) {
        return safeAmount(value).setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        if (hasText(fallback)) {
            return fallback.trim();
        }
        return "";
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private record DocumentFinancialSnapshot(
            BigDecimal totalChargeAmount,
            BigDecimal totalTaxAmount,
            BigDecimal totalPaymentAmount,
            BigDecimal totalChargesIncludingTax,
            BigDecimal latestBalance
    ) {
    }

    private record BalanceSnapshot(
            BigDecimal totalCharges,
            BigDecimal totalPayment,
            BigDecimal balance
    ) {
    }

    private record FolioState(
            String confirmationNo,
            String guestName,
            String roomNo,
            BigDecimal totalCharges,
            BigDecimal totalPayment,
            BigDecimal outstandingBalance,
            Instant createdAt,
            Instant lastUpdatedAt
    ) {
        private BalanceSnapshot snapshot() {
            return new BalanceSnapshot(totalCharges, totalPayment, outstandingBalance);
        }
    }
}
