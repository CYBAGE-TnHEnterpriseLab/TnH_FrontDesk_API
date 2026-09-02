package com.folio.billing.service.impl;

import com.folio.billing.client.ReservationServiceClient;
import com.folio.billing.dto.BillingComments;
import com.folio.billing.dto.BillingDetailsResponse;
import com.folio.billing.dto.BillingTotals;
import com.folio.billing.dto.ChargeAdjustmentType;
import com.folio.billing.dto.FolioBillingFilter;
import com.folio.billing.dto.FolioBillingRow;
import com.folio.billing.dto.FolioCreateRequest;
import com.folio.billing.dto.FolioCreateResponse;
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
import com.folio.billing.dto.FolioDetailsResponse;
import com.folio.billing.dto.FolioPaymentAllocationLineResult;
import com.folio.billing.dto.FolioPaymentAllocationRequest;
import com.folio.billing.dto.FolioPaymentAllocationResponse;
import com.folio.billing.dto.FolioTransactionRow;
import com.folio.billing.dto.FolioTransactionAmountUpdateRequest;
import com.folio.billing.dto.FolioTransactionAmountUpdateResponse;
import com.folio.billing.dto.GuestDetail;
import com.folio.billing.dto.PaymentAllocationHistoryEntry;
import com.folio.billing.dto.PaymentAllocationTargetRequest;
import com.folio.billing.dto.ReservationSummary;
import com.folio.billing.entity.Folio;
import com.folio.billing.repository.FolioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.folio.billing.service.BillingFolioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class BillingFolioServiceImpl implements BillingFolioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingFolioServiceImpl.class);

    private static final BillingTotals ZERO_TOTALS = new BillingTotals(BigDecimal.ZERO, BigDecimal.ZERO);
    private static final String DEFAULT_FOLIO_CODE = "A";
    private static final String TRANSACTION_TYPE_CHARGE = "Charge";
    private static final String TRANSACTION_TYPE_CHARGE_ADJUSTMENT = "ChargeAdjustment";
    private static final String TRANSACTION_TYPE_PAYMENT = "Payment";
    private static final String DOCUMENT_CONTENT_TYPE = "text/html;charset=UTF-8";

    private final ReservationServiceClient reservationServiceClient;
    private final ObjectMapper objectMapper;
    private final FolioRepository folioRepository;
    private final ConcurrentMap<String, FolioState> foliosByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> activeFolioByConfirmationNumber = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<FolioTransactionRow>> postedTransactionsByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<PaymentAllocationHistoryEntry>> allocationHistoryByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FolioDocumentContent> documentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<FolioDocumentAuditEntry>> documentAuditHistoryByConfirmationNumber = new ConcurrentHashMap<>();
    private final AtomicLong chargeReferenceSequence = new AtomicLong(100000L);
    private final AtomicLong adjustmentReferenceSequence = new AtomicLong(200000L);
    private final AtomicLong paymentTransactionReferenceSequence = new AtomicLong(300000L);
    private final AtomicLong paymentReferenceSequence = new AtomicLong(400000L);
    private final AtomicLong documentSequence = new AtomicLong(500000L);

    @Autowired
    public BillingFolioServiceImpl(
            ReservationServiceClient reservationServiceClient,
            ObjectMapper objectMapper, FolioRepository folioRepository
    ) {
        this.reservationServiceClient = reservationServiceClient;
        this.objectMapper = objectMapper;
        this.folioRepository = folioRepository;
        if (folioRepository != null) folioRepository.findAll().forEach(f -> {
            foliosByKey.put(f.getConfirmationNumber() + ":" + f.getFolioCode(),
                new FolioState(f.getConfirmationNumber(), f.getFolioCode(), f.getGuestName(), f.getRoomNo(),
                        f.getTotalCharges(), f.getTotalPayment(), f.getOutstandingBalance(), f.getCreatedAt(), f.getLastUpdatedAt()));
        });
    }

    @Override
    @Transactional
    public List<FolioBillingRow> getFolioBilling(FolioBillingFilter filter) {
        if (filter == null) {
            return List.of();
        }

        List<FolioBillingRow> folioRows = reservationServiceClient.searchFolioBilling(filter);

        // Folio entries are created/maintained automatically whenever reservations are listed.
        folioRows.forEach(this::safeAutoCreateOrRefreshFolio);

        return folioRows;
    }

    private void safeAutoCreateOrRefreshFolio(FolioBillingRow row) {
        try {
            autoCreateOrRefreshFolio(row);
        } catch (Exception ex) {
            LOGGER.warn("Skipping folio auto-refresh for confirmationNumber {} due to persistence error: {}",
                    row == null ? "" : defaultString(row.confirmationNumber()), ex.getMessage());
        }
    }

    public BillingFolioServiceImpl(ReservationServiceClient reservationServiceClient,
            ObjectMapper objectMapper) {
        this(reservationServiceClient, objectMapper, null);
    }

    @Override
    public FolioCreateResponse addFolio(FolioCreateRequest request) {
        if (request == null) {
            throw badRequest("request is required");
        }

        String confirmationNumber = normalize(request.confirmationNumber());
        if (!hasText(confirmationNumber)) {
            throw badRequest("confirmationNumber is required");
        }

        String folioCode = nextAvailableFolioCode(confirmationNumber);
        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNumber, request.roomNo(), request.guestName())
                .orElseGet(() -> emptySummary(request.guestName(), confirmationNumber, request.roomNo()));

        Instant now = Instant.now();
        FolioState folioState = upsertFolio(
                confirmationNumber,
                folioCode,
                firstNonBlank(summary.guestName(), request.guestName()),
                firstNonBlank(summary.roomNo(), request.roomNo()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                now
        );
        activeFolioByConfirmationNumber.put(confirmationNumber, folioCode);

        return new FolioCreateResponse(
                folioState.confirmationNumber(),
                folioState.guestName(),
                folioState.roomNo(),
                folioState.totalCharges(),
                folioState.totalPayment(),
                folioState.outstandingBalance(),
                folioState.createdAt(),
                folioState.lastUpdatedAt(),
                firstNonBlank(request.userId(), "frontdesk-agent")
        );
    }

    @Override
    @Transactional
    public BillingDetailsResponse getBillingDetails(String confirmationNumber, String roomNo, String guestName) {
        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNumber, roomNo, guestName)
                .orElseGet(() -> emptySummary(guestName, confirmationNumber, roomNo));

        String resolvedConfirmationNumber = firstNonBlank(summary.confirmationNumber(), normalize(confirmationNumber));
        String resolvedRoomNo = firstNonBlank(summary.roomNo(), normalize(roomNo));
        // Reservation accommodation is always posted to the primary folio.
        // Additional folios receive only transactions explicitly posted to them.
        String resolvedFolioCode = DEFAULT_FOLIO_CODE;
        ensureReservationCharge(resolvedConfirmationNumber, resolvedFolioCode, summary);

        BalanceSnapshot balanceSnapshot = safeSyncFolioWithLatestBalances(
                resolvedConfirmationNumber,
                resolvedFolioCode,
                summary.guestName(),
                resolvedRoomNo,
                null
        );
        BalanceSnapshot reservationTotals = aggregateFolioBalances(resolvedConfirmationNumber);

        String responseConfirmationNumber = firstNonBlank(summary.confirmationNumber(), resolvedConfirmationNumber);
        List<String> folios = getFoliosForConfirmationNumber(resolvedConfirmationNumber);

        return new BillingDetailsResponse(
                reservationTotals.totalCharges(),
                reservationTotals.totalPayment(),
                reservationTotals.balance(),
                folios,
                resolvedFolioCode,
                defaultString(summary.guestName()),
                defaultString(summary.guest1()),
                defaultString(summary.guest2()),
                defaultString(responseConfirmationNumber),
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
                summary.comments() == null
                    ? new BillingComments(List.of(), "")
                    : new BillingComments(
                        summary.comments().guestRequests() == null ? List.of() : summary.comments().guestRequests(),
                        defaultString(summary.comments().billingComments())
                    )
        );
    }

    private BalanceSnapshot safeSyncFolioWithLatestBalances(
            String confirmationNumber,
            String folioCode,
            String guestName,
            String roomNo,
            List<FolioTransactionRow> knownTransactions
    ) {
        try {
            return syncFolioWithLatestBalances(confirmationNumber, folioCode, guestName, roomNo, knownTransactions);
        } catch (Exception ex) {
            LOGGER.warn("Skipping folio persistence during billing details for confirmationNumber {} due to: {}",
                    defaultString(confirmationNumber), ex.getMessage());

            BillingTotals totals = resolveTotals(confirmationNumber, folioCode, knownTransactions);
            BigDecimal totalCharges = safeAmount(totals.totalCharges());
            BigDecimal totalPayment = safeAmount(totals.totalPayment());
            return new BalanceSnapshot(totalCharges, totalPayment, totalCharges.subtract(totalPayment));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FolioDashboardResponse getFolioDashboard(String confirmationNumber) {
        String resolvedConfirmationNumber = normalize(confirmationNumber);
        String resolvedFolioCode = resolveActiveFolioCode(resolvedConfirmationNumber);

        if (!hasText(resolvedConfirmationNumber)) {
            resolvedConfirmationNumber = reservationServiceClient.findDefaultConfirmationNumber().orElse("");
        }

        if (!hasText(resolvedConfirmationNumber)) {
            return new FolioDashboardResponse(List.of(), List.of());
        }

        List<FolioTransactionRow> transactions = getMergedTransactions(resolvedConfirmationNumber, resolvedFolioCode, null);

        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(resolvedConfirmationNumber, null, null)
            .orElse(emptySummary("", resolvedConfirmationNumber, ""));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                resolvedConfirmationNumber,
                resolvedFolioCode,
                summary.guestName(),
                summary.roomNo(),
                transactions
        );

        List<GuestDetail> rawGuestDetails = Optional
                .ofNullable(reservationServiceClient.getGuestDetails(resolvedConfirmationNumber))
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
    @Transactional
    public FolioDetailsResponse getFolioDetails(String confirmationNumber) {
        String cn = normalize(confirmationNumber);
        if (!hasText(cn)) {
            return new FolioDetailsResponse("", new FolioDetailsResponse.Guest("", ""), List.of(),
                new FolioDetailsResponse.Summary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO));
        }

        try {
            ReservationSummary reservationSummary = reservationServiceClient
                .getReservationSummary(cn, null, null).orElse(null);
            if (reservationSummary != null) {
                ensureReservationCharge(cn, DEFAULT_FOLIO_CODE, reservationSummary);
            }

            List<Folio> persistedFolios = folioRepository.findByConfirmationNumberOrderByFolioCode(cn);
            persistedFolios.stream().filter(Objects::nonNull)
                    .forEach(f -> loadPersistedTransactionsIfNeeded(cn, f.getFolioCode()));
            List<FolioDetailsResponse.Folio> result = persistedFolios.stream()
                .filter(f -> f != null)
                .map(f -> new FolioDetailsResponse.Folio(
                    "FOLIO-" + defaultString(f.getFolioCode()) + "-001",
                    "FOLIO " + defaultString(f.getFolioCode()),
                    defaultString(f.getFolioCode()).equals(activeFolioByConfirmationNumber.getOrDefault(cn, "A")),
                    safeAmount(f.getOutstandingBalance()),
                    safeAmount(f.getTotalCharges()),
                    safeAmount(f.getTotalPayment()),
                    getMergedTransactions(cn, f.getFolioCode(), null).stream()
                        .filter(t -> t != null)
                        .map(t -> new FolioDetailsResponse.Transaction(
                            t.referenceNumber(),
                            t.date(),
                            t.referenceNumber(),
                            t.transactionType(),
                            t.category(),
                            t.description(),
                            safeAmount(t.charges()),
                            safeAmount(t.credit()),
                            t.userId()
                        ))
                        .toList()
                ))
                .toList();

            BigDecimal charges = result.stream().map(FolioDetailsResponse.Folio::totalCharges).map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credits = result.stream().map(FolioDetailsResponse.Folio::totalCredits).map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = result.stream().map(FolioDetailsResponse.Folio::balance).map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Folio first = findPersistedFolio(cn, "A").orElse(null);
            return new FolioDetailsResponse(cn,
                new FolioDetailsResponse.Guest(first == null ? "" : first.getGuestName(), first == null ? "" : first.getRoomNo()),
                result,
                new FolioDetailsResponse.Summary(result.size(), balance, charges, credits,
                    reservationSummary == null ? BigDecimal.ZERO : safeAmount(reservationSummary.taxPercent()),
                    reservationSummary == null ? BigDecimal.ZERO : safeAmount(reservationSummary.taxAmount())));
        } catch (Exception ex) {
            LOGGER.warn("Falling back to in-memory folio details for confirmationNumber {} due to: {}", cn, ex.getMessage());
            return buildFolioDetailsFromState(cn);
        }
    }

        private FolioDetailsResponse buildFolioDetailsFromState(String confirmationNumber) {
        List<FolioState> states = foliosByKey.values().stream()
            .filter(state -> state != null)
            .filter(state -> confirmationNumber.equals(defaultString(state.confirmationNumber())))
            .sorted((left, right) -> defaultString(left.folioCode()).compareToIgnoreCase(defaultString(right.folioCode())))
            .toList();

        List<FolioDetailsResponse.Folio> folios = states.stream()
            .map(state -> {
                String folioCode = defaultString(state.folioCode());
                String key = folioKey(confirmationNumber, folioCode);
                List<FolioDetailsResponse.Transaction> transactions = postedTransactionsByKey.getOrDefault(key, List.of()).stream()
                    .filter(transaction -> transaction != null)
                    .map(transaction -> new FolioDetailsResponse.Transaction(
                        transaction.referenceNumber(),
                        transaction.date(),
                        transaction.referenceNumber(),
                        transaction.transactionType(),
                        transaction.category(),
                        transaction.description(),
                        safeAmount(transaction.charges()),
                        safeAmount(transaction.credit()),
                        transaction.userId()
                    ))
                    .toList();

                return new FolioDetailsResponse.Folio(
                    "FOLIO-" + folioCode + "-001",
                    "FOLIO " + folioCode,
                    folioCode.equals(activeFolioByConfirmationNumber.getOrDefault(confirmationNumber, "A")),
                    safeAmount(state.outstandingBalance()),
                    safeAmount(state.totalCharges()),
                    safeAmount(state.totalPayment()),
                    transactions
                );
            })
            .toList();

        BigDecimal charges = folios.stream().map(FolioDetailsResponse.Folio::totalCharges).map(BillingFolioServiceImpl::safeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = folios.stream().map(FolioDetailsResponse.Folio::totalCredits).map(BillingFolioServiceImpl::safeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = folios.stream().map(FolioDetailsResponse.Folio::balance).map(BillingFolioServiceImpl::safeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        FolioState first = states.isEmpty() ? null : states.get(0);
        return new FolioDetailsResponse(
            confirmationNumber,
            new FolioDetailsResponse.Guest(first == null ? "" : defaultString(first.guestName()), first == null ? "" : defaultString(first.roomNo())),
            folios,
            new FolioDetailsResponse.Summary(folios.size(), balance, charges, credits,
                BigDecimal.ZERO, BigDecimal.ZERO)
        );
        }

    @Override
    @Transactional
    public FolioChargePostResponse addCharge(FolioChargePostRequest request) {
        String confirmationNumber = normalize(request.confirmationNumber());
        if (!hasText(confirmationNumber)) {
            throw badRequest("confirmationNumber is required");
        }

        String transactionType = canonicalTransactionType(request.transactionType());
        String requestedFolioCode = firstNonBlank(folioCodeFromId(request.folioId()), folioCodeFromId(request.folioName()));
        String folioCode = hasText(requestedFolioCode) ? requestedFolioCode : resolveActiveFolioCode(confirmationNumber);
        activeFolioByConfirmationNumber.put(confirmationNumber, folioCode);
        BigDecimal amount = scaleMoney(safeAmount(request.amount()));
        validateChargeAmounts(amount);

        String category = normalizeCategoryForTransactionType(transactionType, request.category());
        validateTransactionCategory(transactionType, category);
        String description = firstNonBlank(request.description(), category + " charge");
        LocalDate postingDate = request.postingDate() != null ? request.postingDate() : LocalDate.now();
        LocalDateTime postedAt = LocalDateTime.now();
        String userId = firstNonBlank(request.userId(), "frontdesk-agent");
        String referenceNumber = generateChargeReference();
        BigDecimal charges = "PAYMENT".equals(transactionType)
                || "ADJUSTMENT".equals(transactionType)
                || "REFUND".equals(transactionType)
                ? BigDecimal.ZERO
                : amount;
        BigDecimal credit = "PAYMENT".equals(transactionType)
                || "ADJUSTMENT".equals(transactionType)
                || "REFUND".equals(transactionType)
                ? amount
                : BigDecimal.ZERO;

        FolioTransactionRow chargeTransaction = new FolioTransactionRow(
                postingDate,
                referenceNumber,
                transactionType,
                category,
                description,
                charges,
                credit,
                userId,
                postedAt,
                null,
                null
        );

        appendPostedTransaction(confirmationNumber, folioCode, chargeTransaction);

        ReservationSummary summary;
        try {
            summary = reservationServiceClient
                    .getReservationSummary(confirmationNumber, request.roomNo(), request.guestName())
                    .orElseGet(() -> emptySummary(request.guestName(), confirmationNumber, request.roomNo()));
        } catch (Exception ex) {
            LOGGER.warn("Unable to load reservation summary after posting transaction for confirmationNumber {}: {}",
                    confirmationNumber, ex.getMessage());
            summary = emptySummary(request.guestName(), confirmationNumber, request.roomNo());
        }

        BalanceSnapshot balanceSnapshot = safeSyncFolioWithLatestBalances(
                confirmationNumber,
                folioCode,
                firstNonBlank(summary.guestName(), request.guestName()),
                firstNonBlank(summary.roomNo(), request.roomNo()),
                null
        );

        String folioId = "FOLIO-" + folioCode + "-001";
        List<FolioChargePostResponse.Transaction> transactionHistory = postedTransactionsByKey
                .getOrDefault(folioKey(confirmationNumber, folioCode), List.of()).stream()
                .map(t -> new FolioChargePostResponse.Transaction(
                        t.referenceNumber(), t.referenceNumber(), t.transactionType(), t.category(),
                        t.description(), safeAmount(t.charges()).add(safeAmount(t.credit())),
                        safeAmount(t.charges()), safeAmount(t.credit()), t.date(), t.userId()))
                .toList();
        return new FolioChargePostResponse(
                confirmationNumber,
                folioId,
                "FOLIO " + folioCode,
                new FolioChargePostResponse.Transaction(referenceNumber, referenceNumber, transactionType, category, description,
                        amount, charges, credit, postingDate, userId),
                new FolioChargePostResponse.FolioSummary(balanceSnapshot.totalCharges(), balanceSnapshot.totalPayment(), balanceSnapshot.balance()),
                new FolioChargePostResponse.CheckoutSummary(balanceSnapshot.balance(), balanceSnapshot.balance().compareTo(BigDecimal.ZERO) == 0),
                referenceNumber,
                transactionType,
                category,
                description,
                amount,
                amount,
                postingDate,
                balanceSnapshot.totalCharges(),
                balanceSnapshot.totalPayment(),
                balanceSnapshot.balance(),
                transactionHistory
        );
    }

    @Override
    public FolioChargeAdjustmentResponse adjustCharge(FolioChargeAdjustmentRequest request) {
        String confirmationNumber = normalize(request.confirmationNumber());
        String folioCode = resolveActiveFolioCode(confirmationNumber);
        if (!hasText(confirmationNumber)) {
            throw badRequest("confirmationNumber is required");
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
        validateChargeAmounts(amount);

        List<FolioTransactionRow> existingTransactions = getMergedTransactions(confirmationNumber, folioCode, null);

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

        BigDecimal signedAmount = request.adjustmentType() == ChargeAdjustmentType.DECREASE
                ? amount.negate()
                : amount;

        BigDecimal currentNetChargeForReference = calculateCurrentNetChargeForReference(
                originalReferenceNumber,
                existingTransactions
        );
        BigDecimal adjustmentTotal = signedAmount;

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
                BigDecimal.ZERO,
                userId,
                postedAt,
                originalReferenceNumber,
                request.reason().trim()
        );

        appendPostedTransaction(confirmationNumber, folioCode, adjustmentTransaction);

        ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNumber, null, null)
                .orElseGet(() -> emptySummary("", confirmationNumber, ""));

        BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                confirmationNumber,
                folioCode,
                summary.guestName(),
                summary.roomNo(),
                null
        );

        return new FolioChargeAdjustmentResponse(
                confirmationNumber,
                originalReferenceNumber,
                adjustmentReferenceNumber,
                request.adjustmentType(),
                category,
                request.reason().trim(),
                amount,
                signedAmount,
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

        Map<String, BigDecimal> allocationsByconfirmationNumber = new LinkedHashMap<>();
        Map<String, String> folioCodesByconfirmationNumber = new LinkedHashMap<>();
        for (PaymentAllocationTargetRequest allocation : requestedAllocations) {
            if (allocation == null) {
                throw badRequest("allocation entry is required");
            }

            String confirmationNumber = normalize(allocation.confirmationNumber());
            String folioCode = resolveActiveFolioCode(confirmationNumber);
            if (!hasText(confirmationNumber)) {
                throw badRequest("confirmationNumber is required for all allocations");
            }

            BigDecimal allocationAmount = scaleMoney(safeAmount(allocation.amount()));
            if (allocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("allocation amount must be greater than zero");
            }

            allocationsByconfirmationNumber.merge(confirmationNumber, allocationAmount, BigDecimal::add);
            folioCodesByconfirmationNumber.putIfAbsent(confirmationNumber, folioCode);
        }

        BigDecimal totalAllocatedAmount = scaleMoney(
                allocationsByconfirmationNumber.values().stream()
                        .map(BillingFolioServiceImpl::safeAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        if (totalAllocatedAmount.compareTo(paymentAmount) > 0) {
            throw badRequest("allocated amount exceeds payment amount");
        }

        Map<String, BigDecimal> balancesBeforeAllocation = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> allocationEntry : allocationsByconfirmationNumber.entrySet()) {
            String confirmationNumber = allocationEntry.getKey();
            String folioCode = folioCodesByconfirmationNumber.getOrDefault(confirmationNumber, DEFAULT_FOLIO_CODE);
            BigDecimal requestedAmount = scaleMoney(allocationEntry.getValue());
            BalanceSnapshot snapshot = syncFolioWithLatestBalances(confirmationNumber, folioCode, "", "", null);
            BigDecimal outstandingBalance = scaleMoney(safeAmount(snapshot.balance()));

            if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("folio has no outstanding balance for confirmationNumber: " + confirmationNumber);
            }

            if (requestedAmount.compareTo(outstandingBalance) > 0) {
                throw badRequest("allocation amount exceeds folio outstanding balance for confirmationNumber: " + confirmationNumber);
            }

            balancesBeforeAllocation.put(confirmationNumber, outstandingBalance);
        }

        String paymentReference = firstNonBlank(request.paymentReference(), generatePaymentReference());
        String paymentMethod = firstNonBlank(request.paymentMethod(), "Card");
        LocalDate allocationDate = request.allocationDate() != null ? request.allocationDate() : LocalDate.now();
        LocalDateTime allocatedAt = LocalDateTime.now();
        String userId = firstNonBlank(request.userId(), "cashier-agent");
        String note = defaultString(request.note());
        BigDecimal unallocatedAmount = scaleMoney(paymentAmount.subtract(totalAllocatedAmount));

        List<FolioPaymentAllocationLineResult> allocationResults = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> allocationEntry : allocationsByconfirmationNumber.entrySet()) {
            String confirmationNumber = allocationEntry.getKey();
            String folioCode = folioCodesByconfirmationNumber.getOrDefault(confirmationNumber, DEFAULT_FOLIO_CODE);
            BigDecimal allocatedAmount = scaleMoney(allocationEntry.getValue());
            BigDecimal balanceBeforeAllocation = balancesBeforeAllocation.getOrDefault(confirmationNumber, BigDecimal.ZERO);
            String transactionReferenceNumber = generatePaymentTransactionReference();

            FolioTransactionRow paymentTransaction = new FolioTransactionRow(
                    allocationDate,
                    transactionReferenceNumber,
                    TRANSACTION_TYPE_PAYMENT,
                    paymentMethod,
                    "Payment allocation " + paymentReference,
                    BigDecimal.ZERO,
                    allocatedAmount,
                    userId,
                    allocatedAt,
                    paymentReference,
                    note
            );

            appendPostedTransaction(confirmationNumber, folioCode, paymentTransaction);

            ReservationSummary summary = reservationServiceClient
                    .getReservationSummary(confirmationNumber, null, null)
                    .orElseGet(() -> emptySummary("", confirmationNumber, ""));

            BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                    confirmationNumber,
                    folioCode,
                    summary.guestName(),
                    summary.roomNo(),
                    null
            );

            allocationResults.add(new FolioPaymentAllocationLineResult(
                    confirmationNumber,
                    transactionReferenceNumber,
                    allocatedAmount,
                    balanceBeforeAllocation,
                    balanceSnapshot.balance()
            ));

            appendAllocationHistory(
                    confirmationNumber,
                    folioCode,
                    new PaymentAllocationHistoryEntry(
                            paymentReference,
                            confirmationNumber,
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
    public List<PaymentAllocationHistoryEntry> getPaymentAllocationHistory(String confirmationNumber, String paymentReference) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);
        String normalizedPaymentReference = normalize(paymentReference);

        Stream<PaymentAllocationHistoryEntry> historyStream = hasText(normalizedConfirmationNumber)
                ? allocationHistoryByKey.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(normalizedConfirmationNumber + ":"))
                        .flatMap(entry -> entry.getValue().stream())
                : allocationHistoryByKey.values().stream().flatMap(List::stream);

        if (hasText(normalizedPaymentReference)) {
            historyStream = historyStream.filter(historyEntry ->
                    normalizedPaymentReference.equalsIgnoreCase(defaultString(historyEntry.paymentReference()))
            );
        }

        return historyStream
                .sorted(Comparator
                        .comparing(PaymentAllocationHistoryEntry::allocatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PaymentAllocationHistoryEntry::paymentReference, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PaymentAllocationHistoryEntry::confirmationNumber, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

            @Override
            public FolioDocumentGenerateResponse generateFolioDocument(FolioDocumentGenerateRequest request) {
            if (request == null) {
                throw badRequest("request is required");
            }

            String confirmationNumber = normalize(request.confirmationNumber());
            if (!hasText(confirmationNumber)) {
                throw badRequest("confirmationNumber is required");
            }

            if (request.documentType() == null) {
                throw badRequest("documentType is required");
            }

            String generatedBy = firstNonBlank(request.userId(), "frontdesk-agent");
            LocalDateTime generatedAt = LocalDateTime.now();

            ReservationSummary summary = reservationServiceClient
                .getReservationSummary(confirmationNumber, null, null)
                .orElseGet(() -> emptySummary("", confirmationNumber, ""));

        List<FolioTransactionRow> transactions = getMergedTransactions(confirmationNumber, DEFAULT_FOLIO_CODE, null);
            BalanceSnapshot balanceSnapshot = syncFolioWithLatestBalances(
                confirmationNumber,
                DEFAULT_FOLIO_CODE,
                summary.guestName(),
                summary.roomNo(),
                transactions
            );
            DocumentFinancialSnapshot financialSnapshot = buildDocumentFinancialSnapshot(transactions, balanceSnapshot);

            String documentId = generateDocumentId();
            String fileName = buildDocumentFileName(request.documentType(), confirmationNumber, documentId);
            String content = buildDocumentHtml(
                documentId,
                confirmationNumber,
                request.documentType(),
                generatedBy,
                generatedAt,
                summary,
                transactions,
                financialSnapshot
            );

            FolioDocumentContent documentContent = new FolioDocumentContent(
                documentId,
                confirmationNumber,
                request.documentType(),
                fileName,
                DOCUMENT_CONTENT_TYPE,
                content,
                generatedAt,
                generatedBy
            );
            documentsById.put(documentId, documentContent);

            appendDocumentAuditHistory(
                confirmationNumber,
                new FolioDocumentAuditEntry(
                    documentId,
                    confirmationNumber,
                    request.documentType(),
                    fileName,
                    generatedAt,
                    generatedBy,
                    financialSnapshot.totalChargeAmount(),
                    financialSnapshot.totalPaymentAmount(),
                    financialSnapshot.latestBalance()
                )
            );

            String downloadPath = "/api/v1/billingFolio/documents/" + documentId + "/download";
            String printPath = "/api/v1/billingFolio/documents/" + documentId + "/print";

            return new FolioDocumentGenerateResponse(
                documentId,
                confirmationNumber,
                request.documentType(),
                fileName,
                DOCUMENT_CONTENT_TYPE,
                generatedAt,
                generatedBy,
                financialSnapshot.totalChargeAmount(),
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
            public List<FolioDocumentAuditEntry> getFolioDocumentAuditHistory(String confirmationNumber, FolioDocumentType documentType) {
            String normalizedConfirmationNumber = normalize(confirmationNumber);

            Stream<FolioDocumentAuditEntry> historyStream = hasText(normalizedConfirmationNumber)
                ? documentAuditHistoryByConfirmationNumber.getOrDefault(normalizedConfirmationNumber, List.of()).stream()
                : documentAuditHistoryByConfirmationNumber.values().stream().flatMap(List::stream);

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
        if (row == null || !hasText(row.confirmationNumber())) {
            return;
        }

        syncFolioWithLatestBalances(
                row.confirmationNumber(),
                DEFAULT_FOLIO_CODE,
                row.guest(),
                row.room(),
                null
        );
    }

    private String folioCodeFromId(String folioId) {
        if (!hasText(folioId)) {
            return "";
        }

        String value = folioId.trim().toUpperCase();
        if (value.length() == 1 && Character.isLetter(value.charAt(0))) {
            return value;
        }

        String letters = value.replaceAll("[^A-Z]", "");
        if (letters.startsWith("FOLIO") && letters.length() > 5) {
            return letters.substring(letters.length() - 1);
        }
        return "";
    }

    private String canonicalTransactionType(String type) {
        String value = firstNonBlank(type, "CHARGE").trim().toUpperCase();
        return switch (value) {
            case "CHARGE", "CHARGES" -> "CHARGE";
            case "PAYMENT" -> "PAYMENT";
            case "ADJUSTMENT" -> "ADJUSTMENT";
            case "REFUND" -> "REFUND";
            default -> "CHARGE";
        };
    }

    private String normalizeCategoryForTransactionType(String transactionType, String category) {
        String raw = firstNonBlank(category, "").trim();
        String normalized = raw.toUpperCase().replace('-', '_').replace(' ', '_');

        if ("CHARGE".equals(transactionType)) {
            if (List.of("F&B", "ACCOMMODATION", "ACCOMODATION", "WELLNESS", "HOUSEKEEPING", "TRANSPORT", "MISCELLANEOUS").contains(normalized)) {
                return raw;
            }
            return "Miscellaneous";
        }
        if ("PAYMENT".equals(transactionType)) {
            if (List.of("UPI", "CREDIT_CARD", "CASH", "DEBIT_CARD", "NET_BANKING").contains(normalized)) {
                return raw;
            }
            return "Cash";
        }
        if ("ADJUSTMENT".equals(transactionType)) {
            return "Discount";
        }
        if ("REFUND".equals(transactionType)) {
            return "Refund";
        }
        return "Miscellaneous";
    }

    private void validateTransactionCategory(String type, String category) {
        String value = firstNonBlank(category, "").toUpperCase().replace('-', '_').replace(' ', '_');
        boolean valid = switch (type) {
            case "CHARGE", "CHARGES" -> List.of("F&B", "ACCOMMODATION", "ACCOMODATION", "WELLNESS", "HOUSEKEEPING", "TRANSPORT", "MISCELLANEOUS").contains(value);
            case "PAYMENT" -> List.of("UPI", "CREDIT_CARD", "CASH", "DEBIT_CARD", "NET_BANKING").contains(value);
            case "ADJUSTMENT" -> "DISCOUNT".equals(value);
            case "REFUND" -> "REFUND".equals(value);
            default -> false;
        };
        if (!valid) throw badRequest("Invalid category for transactionType: " + type);
    }

    private BalanceSnapshot syncFolioWithLatestBalances(
            String confirmationNumber,
            String folioCode,
            String guestName,
            String roomNo,
            List<FolioTransactionRow> knownTransactions
    ) {
        BillingTotals totals = resolveTotals(confirmationNumber, folioCode, knownTransactions);
        BigDecimal totalCharges = safeAmount(totals.totalCharges());
        BigDecimal totalPayment = safeAmount(totals.totalPayment());

        if (!hasText(confirmationNumber)) {
            return new BalanceSnapshot(totalCharges, totalPayment, totalCharges.subtract(totalPayment));
        }

        FolioState folioState = upsertFolio(
                confirmationNumber,
                folioCode,
                guestName,
                roomNo,
                totalCharges,
                totalPayment
        );

        return folioState.snapshot();
    }

    private BillingTotals resolveTotals(String confirmationNumber, String folioCode, List<FolioTransactionRow> knownTransactions) {
        if (!hasText(confirmationNumber)) {
            return ZERO_TOTALS;
        }

        String normalizedconfirmationNumber = normalize(confirmationNumber);
        String normalizedFolioCode = resolveRequestedOrActiveFolioCode(confirmationNumber, folioCode);
        List<FolioTransactionRow> transactions = knownTransactions == null
                ? getMergedTransactions(normalizedconfirmationNumber, normalizedFolioCode, null)
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
        return safeAmount(transaction.charges());
    }

    private List<FolioTransactionRow> getMergedTransactions(String confirmationNumber, String folioCode, List<FolioTransactionRow> knownBaseTransactions) {
        if (!hasText(confirmationNumber)) {
            return List.of();
        }

        String normalizedconfirmationNumber = normalize(confirmationNumber);
        String normalizedFolioCode = resolveRequestedOrActiveFolioCode(confirmationNumber, folioCode);

        List<FolioTransactionRow> baseTransactions = knownBaseTransactions == null ? List.of() : knownBaseTransactions;

        loadPersistedTransactionsIfNeeded(normalizedconfirmationNumber, normalizedFolioCode);

        List<FolioTransactionRow> postedTransactions = postedTransactionsByKey
                .getOrDefault(folioKey(normalizedconfirmationNumber, normalizedFolioCode), List.of());

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

    @Override
    @Transactional
    public FolioTransactionAmountUpdateResponse updateTransactionAmount(FolioTransactionAmountUpdateRequest request) {
        String confirmationNumber = normalize(request.confirmationNumber());
        String folioCode = resolveActiveFolioCode(confirmationNumber);
        String referenceNumber = normalize(request.referenceNumber());
        List<FolioTransactionRow> existing = new ArrayList<>(getMergedTransactions(confirmationNumber, folioCode, null));
        int index = -1;
        for (int i = 0; i < existing.size(); i++) {
            if (referenceNumber.equalsIgnoreCase(defaultString(existing.get(i).referenceNumber()))) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found: " + referenceNumber);
        }

        FolioTransactionRow old = existing.get(index);
        BigDecimal amount = scaleMoney(request.amount());
        boolean creditTransaction = "PAYMENT".equalsIgnoreCase(old.transactionType())
                || "ADJUSTMENT".equalsIgnoreCase(old.transactionType())
                || "REFUND".equalsIgnoreCase(old.transactionType());
        FolioTransactionRow updated = new FolioTransactionRow(
                old.date(), old.referenceNumber(), old.transactionType(), old.category(), old.description(),
                creditTransaction ? BigDecimal.ZERO : amount,
                creditTransaction ? amount : BigDecimal.ZERO,
                firstNonBlank(request.userId(), old.userId()), old.postedAt(), old.originalReferenceNumber(),
                old.adjustmentReason());
        existing.set(index, updated);
        postedTransactionsByKey.put(folioKey(confirmationNumber, folioCode), List.copyOf(existing));

        BillingTotals totals = totalsFromTransactions(existing);
        upsertFolio(confirmationNumber, folioCode, "", "", totals.totalCharges(), totals.totalPayment());
        return new FolioTransactionAmountUpdateResponse(
                confirmationNumber,
                "FOLIO-" + folioCode + "-001",
                new FolioChargePostResponse.Transaction(updated.referenceNumber(), updated.referenceNumber(), updated.transactionType(),
                        updated.category(), updated.description(), amount, updated.charges(), updated.credit(), updated.date(), updated.userId()),
                totals.totalCharges(), totals.totalPayment(), totals.totalCharges().subtract(totals.totalPayment()));
    }

    private void appendPostedTransaction(String confirmationNumber, String folioCode, FolioTransactionRow transaction) {
        String normalizedconfirmationNumber = normalize(confirmationNumber);
        String normalizedFolioCode = resolveRequestedOrActiveFolioCode(confirmationNumber, folioCode);
        String key = folioKey(normalizedconfirmationNumber, normalizedFolioCode);

        loadPersistedTransactionsIfNeeded(normalizedconfirmationNumber, normalizedFolioCode);

        postedTransactionsByKey.compute(key, (ignored, existingTransactions) -> {
            List<FolioTransactionRow> mergedTransactions = existingTransactions == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingTransactions);
            mergedTransactions.add(transaction);
            return List.copyOf(mergedTransactions);
        });
        if (folioRepository != null) {
            try {
                Optional<Folio> persistedFolio = findPersistedFolio(normalizedconfirmationNumber, normalizedFolioCode);
                if (persistedFolio.isPresent()) {
                    Folio f = persistedFolio.get();
                    f.setTransactionsJson(objectMapper.writeValueAsString(postedTransactionsByKey.get(key)));
                    folioRepository.saveAndFlush(f);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to persist folio transaction for confirmationNumber "
                        + normalizedconfirmationNumber + " and folioCode " + normalizedFolioCode, ex);
            }
        }
    }

    private void loadPersistedTransactionsIfNeeded(String confirmationNumber, String folioCode) {
        if (folioRepository == null) {
            return;
        }

        String key = folioKey(confirmationNumber, folioCode);

        try {
            findPersistedFolio(confirmationNumber, folioCode).ifPresent(folio -> {
                String transactionsJson = folio.getTransactionsJson();
                if (transactionsJson == null || transactionsJson.isBlank()) {
                    postedTransactionsByKey.putIfAbsent(key, List.of());
                    return;
                }
                try {
                    List<FolioTransactionRow> transactions = objectMapper.readValue(
                            transactionsJson, new TypeReference<List<FolioTransactionRow>>() {});
                    List<FolioTransactionRow> cleanedTransactions = deduplicateTransactions(
                            transactions == null ? List.of() : transactions);
                    if (!DEFAULT_FOLIO_CODE.equalsIgnoreCase(folio.getFolioCode())) {
                        cleanedTransactions = cleanedTransactions.stream()
                                .filter(transaction -> !isReservationTransaction(transaction))
                                .toList();
                    }
                    List<FolioTransactionRow> inMemoryTransactions = postedTransactionsByKey.getOrDefault(key, List.of());
                    if (!DEFAULT_FOLIO_CODE.equalsIgnoreCase(folio.getFolioCode())) {
                        inMemoryTransactions = inMemoryTransactions.stream()
                                .filter(transaction -> !isReservationTransaction(transaction))
                                .toList();
                    }
                    List<FolioTransactionRow> mergedTransactions = deduplicateTransactions(
                            Stream.concat(cleanedTransactions.stream(), inMemoryTransactions.stream()).toList());
                    postedTransactionsByKey.put(key, mergedTransactions);
                    if (cleanedTransactions.size() != (transactions == null ? 0 : transactions.size())) {
                        BigDecimal charges = mergedTransactions.stream()
                                .map(BillingFolioServiceImpl::chargeComponent)
                                .map(BillingFolioServiceImpl::safeAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal credits = mergedTransactions.stream()
                                .map(FolioTransactionRow::credit)
                                .map(BillingFolioServiceImpl::safeAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        folio.update(folio.getGuestName(), folio.getRoomNo(), charges, credits,
                                charges.subtract(credits), folio.getLastUpdatedAt());
                        folio.setTransactionsJson(objectMapper.writeValueAsString(mergedTransactions));
                        folioRepository.saveAndFlush(folio);
                    }
                } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException ex) {
                    // Older rows may contain a scalar value instead of the current JSON array.
                    // Do not fail the billing-details request; the next folio update will
                    // persist the valid array format and retain any newly posted transaction.
                    LOGGER.warn("Ignoring invalid persisted transactions for {}: expected a JSON array", key);
                    postedTransactionsByKey.putIfAbsent(key, List.of());
                } catch (Exception ex) {
                    throw new IllegalStateException("Unable to deserialize folio transactions for " + key, ex);
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load persisted transactions for confirmationNumber "
                    + confirmationNumber + " and folioCode " + folioCode, ex);
        }
    }

    private List<FolioTransactionRow> deduplicateTransactions(List<FolioTransactionRow> transactions) {
        Map<String, FolioTransactionRow> uniqueByReference = new LinkedHashMap<>();
        for (FolioTransactionRow transaction : transactions) {
            if (transaction == null) {
                continue;
            }
            String reference = normalize(transaction.referenceNumber());
            String identity = hasText(reference)
                    ? "REFERENCE:" + reference.toUpperCase()
                    : "ROW:" + uniqueByReference.size();
            uniqueByReference.putIfAbsent(identity, transaction);
        }
        return List.copyOf(uniqueByReference.values());
    }

    private boolean isReservationTransaction(FolioTransactionRow transaction) {
        return transaction != null
                && defaultString(transaction.referenceNumber())
                .toUpperCase()
                .startsWith("RESERVATION-");
    }

    private void appendAllocationHistory(String confirmationNumber, String folioCode, PaymentAllocationHistoryEntry historyEntry) {
        String normalizedconfirmationNumber = normalize(confirmationNumber);
        String key = folioKey(normalizedconfirmationNumber, folioCode);

        allocationHistoryByKey.compute(key, (ignored, existingEntries) -> {
            List<PaymentAllocationHistoryEntry> mergedEntries = existingEntries == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingEntries);
            mergedEntries.add(historyEntry);
            return List.copyOf(mergedEntries);
        });
    }

    private void appendDocumentAuditHistory(String confirmationNumber, FolioDocumentAuditEntry historyEntry) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);

        documentAuditHistoryByConfirmationNumber.compute(normalizedConfirmationNumber, (key, existingEntries) -> {
            List<FolioDocumentAuditEntry> mergedEntries = existingEntries == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingEntries);
            mergedEntries.add(historyEntry);
            return List.copyOf(mergedEntries);
        });
    }

    private FolioState upsertFolio(
            String confirmationNumber,
            String folioCode,
            String guestName,
            String roomNo,
            BigDecimal totalCharges,
            BigDecimal totalPayment
    ) {
        return upsertFolio(confirmationNumber, folioCode, guestName, roomNo, totalCharges, totalPayment, Instant.now());
    }

    private FolioState upsertFolio(
            String confirmationNumber,
            String folioCode,
            String guestName,
            String roomNo,
            BigDecimal totalCharges,
            BigDecimal totalPayment,
            Instant updatedAt
    ) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);
        String normalizedFolioCode = resolveRequestedOrActiveFolioCode(confirmationNumber, folioCode);
        String key = folioKey(normalizedConfirmationNumber, normalizedFolioCode);
        Instant now = updatedAt == null ? Instant.now() : updatedAt;

        FolioState state = foliosByKey.compute(key, (ignored, existing) -> {
            String resolvedGuestName = firstNonBlank(guestName, existing == null ? "" : existing.guestName());
            String resolvedRoomNo = firstNonBlank(roomNo, existing == null ? "" : existing.roomNo());
            Instant createdAt = existing == null ? now : existing.createdAt();

            return new FolioState(
                    normalizedConfirmationNumber,
                    normalizedFolioCode,
                    resolvedGuestName,
                    resolvedRoomNo,
                    totalCharges,
                    totalPayment,
                    totalCharges.subtract(totalPayment),
                    createdAt,
                    now
            );
        });
        if (folioRepository == null) return state;
        Folio persisted = findPersistedFolio(normalizedConfirmationNumber, normalizedFolioCode)
                .orElseGet(() -> new Folio(normalizedConfirmationNumber, normalizedFolioCode, state.guestName(), state.roomNo(),
                        state.totalCharges(), state.totalPayment(), state.outstandingBalance(), state.createdAt(), state.lastUpdatedAt()));
        persisted.update(state.guestName(), state.roomNo(), state.totalCharges(), state.totalPayment(),
                state.outstandingBalance(), state.lastUpdatedAt());
        try {
            persisted.setTransactionsJson(objectMapper.writeValueAsString(
                    postedTransactionsByKey.getOrDefault(key, List.of())));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize folio transactions for confirmationNumber "
                    + normalizedConfirmationNumber + " and folioCode " + normalizedFolioCode, ex);
        }
        folioRepository.saveAndFlush(persisted);
        return state;
    }

    private String nextAvailableFolioCode(String confirmationNumber) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);
        for (char code = 'A'; code <= 'Z'; code++) {
            String candidate = String.valueOf(code);
            if (!foliosByKey.containsKey(folioKey(normalizedConfirmationNumber, candidate))) {
                return candidate;
            }
        }
        throw badRequest("No folio codes available for confirmationNumber: " + normalizedConfirmationNumber);
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

    private ReservationSummary emptySummary(String guestName, String confirmationNumber, String roomNo) {
        return new ReservationSummary(
                defaultString(guestName),
                "",
                "",
                defaultString(confirmationNumber),
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
                new BillingComments(List.of(), ""),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private void ensureReservationCharge(String confirmationNumber, String folioCode, ReservationSummary summary) {
        BigDecimal amount = scaleMoney(safeAmount(summary.reservationAmount()));
        if (!hasText(confirmationNumber) || amount.signum() <= 0) return;

        loadPersistedTransactionsIfNeeded(confirmationNumber, folioCode);
        List<FolioTransactionRow> existing = postedTransactionsByKey.getOrDefault(
                folioKey(confirmationNumber, folioCode), List.of());
        String reference = "RESERVATION-" + normalize(confirmationNumber);
        int existingIndex = -1;
        for (int index = 0; index < existing.size(); index++) {
            if (reference.equalsIgnoreCase(defaultString(existing.get(index).referenceNumber()))) {
                existingIndex = index;
                break;
            }
        }

        FolioTransactionRow reservationTransaction = new FolioTransactionRow(
                LocalDate.now(), reference, "CHARGE", "ACCOMMODATION",
                "Reservation amount", amount, BigDecimal.ZERO, "reservation-service",
                LocalDateTime.now(), null, null);
        if (existingIndex >= 0) {
            FolioTransactionRow current = existing.get(existingIndex);
            if (safeAmount(current.charges()).compareTo(amount) == 0) {
                return;
            }
            List<FolioTransactionRow> updatedTransactions = new ArrayList<>(existing);
            updatedTransactions.set(existingIndex, reservationTransaction);
            replacePostedTransactions(confirmationNumber, folioCode, updatedTransactions);
            return;
        }
        appendPostedTransaction(confirmationNumber, folioCode, reservationTransaction);
    }

    private void replacePostedTransactions(String confirmationNumber, String folioCode,
                                           List<FolioTransactionRow> transactions) {
        String key = folioKey(confirmationNumber, folioCode);
        postedTransactionsByKey.put(key, List.copyOf(transactions));
        BillingTotals totals = totalsFromTransactions(transactions);
        upsertFolio(confirmationNumber, folioCode, "", "", totals.totalCharges(), totals.totalPayment());
        if (folioRepository == null) {
            return;
        }
        findPersistedFolio(confirmationNumber, folioCode).ifPresent(folio -> {
            try {
                folio.setTransactionsJson(objectMapper.writeValueAsString(transactions));
                folioRepository.saveAndFlush(folio);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to persist reservation charge update", ex);
            }
        });
    }

    private String resolveActiveFolioCode(String confirmationNumber) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);
        if (!hasText(normalizedConfirmationNumber)) {
            return DEFAULT_FOLIO_CODE;
        }
        return activeFolioByConfirmationNumber.getOrDefault(normalizedConfirmationNumber, DEFAULT_FOLIO_CODE);
    }

    private String resolveRequestedOrActiveFolioCode(String confirmationNumber, String folioCode) {
        String normalizedRequested = normalize(folioCode);
        if (hasText(normalizedRequested)) {
            return normalizedRequested.toUpperCase();
        }
        return resolveActiveFolioCode(confirmationNumber);
    }

    private String folioKey(String confirmationNumber, String folioCode) {
        return normalize(confirmationNumber) + ":" + resolveRequestedOrActiveFolioCode(confirmationNumber, folioCode);
    }

    private List<String> getFoliosForConfirmationNumber(String confirmationNumber) {
        String normalizedConfirmationNumber = normalize(confirmationNumber);
        if (!hasText(normalizedConfirmationNumber)) {
            return List.of();
        }

        return foliosByKey.keySet().stream()
                .filter(key -> key.startsWith(normalizedConfirmationNumber + ":"))
                .map(key -> key.substring(key.indexOf(':') + 1))
                .sorted()
                .toList();
    }

    private BalanceSnapshot aggregateFolioBalances(String confirmationNumber) {
        String prefix = normalize(confirmationNumber) + ":";
        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalPayment = BigDecimal.ZERO;

        for (FolioState folio : foliosByKey.values()) {
            if (!folioKey(folio.confirmationNumber(), folio.folioCode()).startsWith(prefix)) {
                continue;
            }
            totalCharges = totalCharges.add(safeAmount(folio.totalCharges()));
            totalPayment = totalPayment.add(safeAmount(folio.totalPayment()));
        }

        return new BalanceSnapshot(totalCharges, totalPayment, totalCharges.subtract(totalPayment));
    }

    private Optional<Folio> findPersistedFolio(String confirmationNumber, String folioCode) {
        if (folioRepository == null || !hasText(confirmationNumber) || !hasText(folioCode)) {
            return Optional.empty();
        }

        return folioRepository.findByConfirmationNumberOrderByFolioCode(confirmationNumber).stream()
                .filter(folio -> folioCode.equalsIgnoreCase(defaultString(folio.getFolioCode())))
                .findFirst();
    }

    private DocumentFinancialSnapshot buildDocumentFinancialSnapshot(
            List<FolioTransactionRow> transactions,
            BalanceSnapshot balanceSnapshot
    ) {
        BigDecimal totalChargeAmount = transactions.stream()
                .map(FolioTransactionRow::charges)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaymentAmount = transactions.stream()
                .map(FolioTransactionRow::credit)
                .map(BillingFolioServiceImpl::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal latestBalance = safeAmount(balanceSnapshot.balance());

        return new DocumentFinancialSnapshot(
                scaleMoney(totalChargeAmount),
                scaleMoney(totalPaymentAmount),
                scaleMoney(latestBalance)
        );
    }

    private String buildDocumentHtml(
            String documentId,
            String confirmationNumber,
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
                .append(escapeHtml(confirmationNumber)).append(" | Generated: ").append(escapeHtml(formatDateTime(generatedAt)))
                .append(" | User: ").append(escapeHtml(generatedBy)).append("</div>")
                .append("<h2>Guest And Reservation Details</h2>")
                .append("<div class=\"grid\"> ");

        appendDetailRow(html, "Guest Name", summary.guestName());
        appendDetailRow(html, "Confirmation No", confirmationNumber);
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
                .append("<h2>Charge And Payment Details</h2>")
                .append("<table><thead><tr>")
                .append("<th>Date</th><th>Reference</th><th>Type</th><th>Category</th><th>Description</th>")
                .append("<th>Charge</th><th>Payment</th><th>User</th><th>Posted At</th>")
                .append("</tr></thead><tbody>");

        if (transactions.isEmpty()) {
            html.append("<tr><td colspan=\"9\">No folio transactions available.</td></tr>");
        } else {
            for (FolioTransactionRow transaction : transactions) {
                html.append("<tr>")
                        .append("<td>").append(escapeHtml(formatDate(transaction.date()))).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.referenceNumber())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.transactionType())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.category())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.description())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(transaction.charges())).append("</td>")
                        .append("<td class=\"num\">").append(formatMoney(transaction.credit())).append("</td>")
                        .append("<td>").append(escapeHtml(transaction.userId())).append("</td>")
                        .append("<td>").append(escapeHtml(formatDateTime(transaction.postedAt()))).append("</td>")
                        .append("</tr>");
            }
        }

        html.append("</tbody></table>")
                .append("<h2>Latest Folio Summary</h2>")
                .append("<div class=\"summary\">")
                .append("<div class=\"label\">Total Charge Amount</div><div class=\"value\">").append(formatMoney(financialSnapshot.totalChargeAmount())).append("</div>")
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

    private void validateChargeAmounts(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("amount must be greater than zero");
        }
    }

    private String generateChargeReference() {
        return "TXN-CHG-" + UUID.randomUUID();
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

    private String buildDocumentFileName(FolioDocumentType documentType, String confirmationNumber, String documentId) {
        String prefix = documentType == FolioDocumentType.RECEIPT ? "receipt" : "invoice";
        return prefix + "-" + confirmationNumber + "-" + documentId + ".html";
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
            BigDecimal totalPaymentAmount,
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
            String confirmationNumber,
            String folioCode,
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

