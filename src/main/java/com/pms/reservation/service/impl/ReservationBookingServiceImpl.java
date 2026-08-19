package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import com.pms.housekeeping.repository.HousekeepingRoomStatusRepository;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.constant.PaymentTypes;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.dto.ReservationViewResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.HousekeepingRoomCalendarClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.reservation.integration.dto.InventoryDeductionRequest;
import com.pms.reservation.integration.dto.InventorySyncRequest;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.integration.dto.PropertyTaxRuleResponseDto;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import com.pms.reservation.service.ReservationBookingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationBookingServiceImpl implements ReservationBookingService {

    private static final String RESERVATION_STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_ARRIVED = "ARRIVED";
    private static final String STATUS_CHECKED_IN = "CHECKED_IN";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String DEFAULT_CURRENCY = "INR";
    private static final int CONFIRMATION_MIN = 1000000000;
    private static final int CONFIRMATION_MAX_EXCLUSIVE = 2000000000;
    private static final int CONFIRMATION_MAX_ATTEMPTS = 50;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationPaymentTransactionRepository reservationPaymentTransactionRepository;
    private final HousekeepingRoomStatusRepository housekeepingRoomStatusRepository;
    private final PropertyInventoryPort propertyInventoryPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationBookingMapper reservationBookingMapper;
    private final PaymentProcessingService paymentProcessingService;
    private final HousekeepingRoomCalendarClient housekeepingRoomCalendarClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request) {
        normalizeCreateRequest(request);
        validatePhoneNumberFormat(request.getPhoneNumber());
        validateDates(request.getArrivalDate(), request.getDepartureDate());
        validateRequiredContactFields(request);
        validateRoomSelectionAndGuestNames(request);
        validateAndNormalizePaymentMode(request);
        validateAndNormalizePaymentType(request);
        String confirmationNumber = generateConfirmationNumber(request.getPropertyId());

        validateRoomAssignments(request);

        BigDecimal payableAmount = calculatePayableAmount(request);
        PaymentProcessingResult paymentResult = paymentProcessingService.processPayment(request, confirmationNumber, payableAmount);
        if (!PAYMENT_STATUS_SUCCESS.equalsIgnoreCase(paymentResult.getStatus())) {
            String failureReason = paymentResult.getFailureReason() == null
                    ? "payment processing failed"
                    : paymentResult.getFailureReason();
            throw new BadRequestException("payment processing failed: " + failureReason);
        }

        ReservationBookingRecord entity = reservationBookingMapper.toEntity(request);
        applyPropertyTaxOnBooking(entity);
        entity.setConfirmationNumber(confirmationNumber);
        entity.setReservationStatus(RESERVATION_STATUS_CONFIRMED);
        ReservationBookingRecord saved = reservationBookingRepository.save(entity);
        markRoomsAssigned(request, confirmationNumber);
        ReservationPaymentTransactionRecord savedPaymentTransaction = reservationPaymentTransactionRepository
            .save(buildPaymentTransaction(saved, request, payableAmount, paymentResult));
        return reservationBookingMapper.toResponse(saved, savedPaymentTransaction);
    }

    @Override
    @Transactional
    public ReservationViewResponseDto updateBooking(String confirmationNumber, ReservationBookingRequestDto request) {
        normalizeCreateRequest(request);
        validatePhoneNumberFormat(request.getPhoneNumber());
        validateDates(request.getArrivalDate(), request.getDepartureDate());
        validateRequiredContactFields(request);
        validateRoomSelectionAndGuestNames(request);

        ReservationBookingRecord existing = reservationBookingRepository.findByConfirmationNumber(confirmationNumber)
            .orElseThrow(() -> new BadRequestException("Reservation booking not found"));

        if (STATUS_CHECKED_OUT.equalsIgnoreCase(existing.getReservationStatus())) {
            throw new BadRequestException("Checked-out reservations cannot be changed. Cancel the same-day check-out to re-check in the guest first");
        }

        request.setPayment(existing.getPayment());
        request.setPaymentType(existing.getPaymentType());

        if (propertyWizardServiceProperties.isEnabled()) {
            validatePropertyAndInventory(request);
        }

        validateRoomAssignments(request);

        ReservationBookingRecord updated = reservationBookingMapper.toEntity(request);
        preserveSystemFields(existing, updated);
        applyPropertyTaxOnBooking(updated);

        ReservationBookingRecord saved = reservationBookingRepository.save(updated);
        markRoomsAssigned(request, confirmationNumber);
        Optional<ReservationPaymentTransactionRecord> latestTransaction =
            reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(existing.getId());
        return buildReservationViewResponse(saved, latestTransaction.orElse(null));
    }

        @Override
        @Transactional(readOnly = true)
        public ReservationViewResponseDto getBookingDetails(String confirmationNumber) {
        ReservationBookingRecord booking = reservationBookingRepository.findByConfirmationNumber(confirmationNumber)
            .orElseThrow(() -> new BadRequestException("Reservation booking not found"));

        Optional<ReservationPaymentTransactionRecord> latestTransaction = booking.getId() == null
            ? Optional.empty()
            : reservationPaymentTransactionRepository.findTopByBookingIdOrderByCreatedAtDesc(booking.getId());
        return buildReservationViewResponse(booking, latestTransaction.orElse(null));
        }

    private ReservationViewResponseDto buildReservationViewResponse(
            ReservationBookingRecord booking,
            ReservationPaymentTransactionRecord paymentTransaction
    ) {
        ReservationBookingResponseDto baseResponse = reservationBookingMapper.toResponse(booking, paymentTransaction);
        LocalDate businessDate = resolveBusinessDate(booking);
        TaxSummary taxSummary = calculateTaxSummary(booking);

        return ReservationViewResponseDto.builder()
                .reservationId(booking.getConfirmationNumber())
                .confirmationNumber(booking.getConfirmationNumber())
                .status(booking.getReservationStatus())
                .createdAt(booking.getCreatedAt() == null ? null : booking.getCreatedAt().atOffset(ZoneOffset.UTC))
                .propertyId(booking.getPropertyId())
                .businessDate(businessDate)
                .guest(buildPrimaryGuest(booking))
                .additionalGuests(buildAdditionalGuests(
                        baseResponse.getGuestNames(),
                        booking.getGuestName(),
                        booking.getPhoneNumber(),
                        preferredEmail(booking)
                ))
                .stay(buildStay(booking))
                .room(buildRoom(booking))
                .booking(buildBookingDetails(booking))
                .pricing(buildPricing(booking, taxSummary))
                .comments(buildComments(booking))
                .actions(buildActions(booking))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationBookingResponseDto> getBookings() {
        List<ReservationBookingRecord> bookings = reservationBookingRepository.findAllByOrderByCreatedAtDesc();
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream()
                .map(ReservationBookingRecord::getId)
                .collect(Collectors.toList());

        Map<Long, ReservationPaymentTransactionRecord> transactionByBookingId = reservationPaymentTransactionRepository
                .findByBookingIdIn(bookingIds)
                .stream()
                .collect(Collectors.toMap(
                        ReservationPaymentTransactionRecord::getBookingId,
                        Function.identity(),
                        (existing, ignored) -> existing
                ));

        return bookings.stream()
                .map(booking -> reservationBookingMapper.toResponse(booking, transactionByBookingId.get(booking.getId())))
                .collect(Collectors.toList());
    }

    private ReservationViewResponseDto.GuestDto buildPrimaryGuest(ReservationBookingRecord booking) {
        String[] nameParts = splitGuestName(booking.getGuestName());
        return ReservationViewResponseDto.GuestDto.builder()
                .salutation(booking.getSalutation())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .phoneNumber(booking.getPhoneNumber())
                .email(preferredEmail(booking))
                .loyaltyNumber(booking.getLoyaltyNumber())
                .build();
    }

    private List<ReservationViewResponseDto.AdditionalGuestDto> buildAdditionalGuests(
            List<String> guestNames,
            String primaryGuestName,
            String fallbackPhone,
            String fallbackEmail
    ) {
        if (guestNames == null || guestNames.isEmpty()) {
            return List.of();
        }

        List<ReservationViewResponseDto.AdditionalGuestDto> additionalGuests = new ArrayList<>();
        boolean primarySkipped = false;
        String normalizedPrimary = normalizeValue(primaryGuestName);

        for (String guestName : guestNames) {
            if (!StringUtils.hasText(guestName)) {
                continue;
            }

            String trimmedName = guestName.trim();
            if (!primarySkipped
                    && StringUtils.hasText(normalizedPrimary)
                    && normalizedPrimary.equals(normalizeValue(trimmedName))) {
                primarySkipped = true;
                continue;
            }

            additionalGuests.add(ReservationViewResponseDto.AdditionalGuestDto.builder()
                    .name(trimmedName)
                    .phoneNumber(fallbackPhone)
                    .email(fallbackEmail)
                    .build());
        }

        return additionalGuests;
    }

    private ReservationViewResponseDto.StayDto buildStay(ReservationBookingRecord booking) {
        int nights = 0;
        if (booking.getArrivalDate() != null && booking.getDepartureDate() != null) {
            nights = Math.max(0, (int) ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate()));
        }

        return ReservationViewResponseDto.StayDto.builder()
                .checkInDate(booking.getArrivalDate())
                .checkOutDate(booking.getDepartureDate())
                .nights(nights)
                .rooms(booking.getNumberOfRooms())
                .adults(booking.getAdultCount())
                .children(booking.getChildCount())
                .childAges(List.of())
                .checkInTime(formatTime(booking.getEta()))
                .checkOutTime(formatTime(booking.getCheckOutTime()))
                .build();
    }

    private ReservationViewResponseDto.RoomDto buildRoom(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.RoomDto.builder()
                .roomNo(booking.getAssignedRoomNo())
                .roomType(booking.getRoomType())
                .floor(booking.getFloor() == null ? null : String.valueOf(booking.getFloor()))
                .roomStatus(resolveRoomStatus(booking))
                .build();
    }

    private ReservationViewResponseDto.BookingDto buildBookingDetails(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.BookingDto.builder()
                .groupCode(booking.getGuestGroup())
                .company(booking.getCompany())
                .blockCode(null)
                .source(booking.getSource())
                .reservationType(booking.getReservationType())
                .rateCode(booking.getRateCode())
                .build();
    }

    private ReservationViewResponseDto.PricingDto buildPricing(ReservationBookingRecord booking, TaxSummary taxSummary) {
        return ReservationViewResponseDto.PricingDto.builder()
                .currency(DEFAULT_CURRENCY)
                .roomRate(booking.getRate())
                .taxPercent(taxSummary.taxPercent)
                .taxAmount(taxSummary.taxAmount)
                .totalRate(booking.getTotalRate())
                .guestBalance(booking.getGuestBalance())
                .discount(booking.getDiscount())
                .build();
    }

    private ReservationViewResponseDto.CommentsDto buildComments(ReservationBookingRecord booking) {
        return ReservationViewResponseDto.CommentsDto.builder()
                .guestRequests(parseGuestRequests(booking.getSpecialRequests()))
                .billingComments(booking.getAlertsMessages())
                .build();
    }

    private ReservationViewResponseDto.ActionsDto buildActions(ReservationBookingRecord booking) {
        String status = normalizeStatus(booking.getReservationStatus());
        boolean canEdit = RESERVATION_STATUS_CONFIRMED.equals(status);
        boolean canCheckIn = RESERVATION_STATUS_CONFIRMED.equals(status);
        boolean canCheckOut = STATUS_ARRIVED.equals(status) || STATUS_CHECKED_IN.equals(status);
        boolean canCancel = false;

        if (STATUS_CHECKED_OUT.equals(status)) {
            canEdit = false;
            canCheckIn = false;
            canCheckOut = false;
        }

        return ReservationViewResponseDto.ActionsDto.builder()
                .canEdit(canEdit)
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .canCancel(canCancel)
                .build();
    }

    private TaxSummary calculateTaxSummary(ReservationBookingRecord booking) {
        BigDecimal baseAmount = calculateBaseTotalRate(booking);
        BigDecimal roomRate = booking.getRate() == null ? BigDecimal.ZERO : booking.getRate();
        if (!propertyWizardServiceProperties.isEnabled()
                || baseAmount.compareTo(BigDecimal.ZERO) <= 0
                || roomRate.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxSummary.zero();
        }

        List<PropertyTaxRuleResponseDto> taxRules = safeFetchTaxRules(booking.getPropertyId());
        PropertyTaxRuleResponseDto matchedRule = findMatchedTaxRule(booking.getRoomType(), roomRate, taxRules);
        if (matchedRule == null) {
            return TaxSummary.zero();
        }

        BigDecimal taxPercent = matchedRule.getTaxPercentage() == null ? BigDecimal.ZERO : matchedRule.getTaxPercentage();
        BigDecimal taxAmount = BigDecimal.ZERO;

        if (matchedRule.getTaxPercentage() != null) {
            taxAmount = baseAmount
                    .multiply(matchedRule.getTaxPercentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        if (matchedRule.getFixedTaxAmount() != null) {
            taxAmount = taxAmount.add(matchedRule.getFixedTaxAmount());
        }

        return new TaxSummary(taxPercent, taxAmount);
    }

    private List<PropertyTaxRuleResponseDto> safeFetchTaxRules(String propertyId) {
        try {
            List<PropertyTaxRuleResponseDto> taxRules = propertyInventoryPort.fetchTaxRules(propertyId);
            return taxRules == null ? List.of() : taxRules;
        } catch (ExternalServiceException ex) {
            return List.of();
        }
    }

    private PropertyTaxRuleResponseDto findMatchedTaxRule(
            String roomType,
            BigDecimal roomRate,
            List<PropertyTaxRuleResponseDto> taxRules
    ) {
        if (taxRules == null || taxRules.isEmpty()) {
            return null;
        }

        return taxRules.stream()
                .filter(rule -> !Boolean.FALSE.equals(rule.getActive()))
                .filter(rule -> !StringUtils.hasText(rule.getRoomType()) || isSameRoomType(rule.getRoomType(), roomType))
                .filter(rule -> rule.getMinAmount() == null || roomRate.compareTo(rule.getMinAmount()) >= 0)
                .filter(rule -> rule.getMaxAmount() == null || roomRate.compareTo(rule.getMaxAmount()) <= 0)
                .findFirst()
                .orElse(null);
    }

    private void applyPropertyTaxOnBooking(ReservationBookingRecord booking) {
        if (booking == null) {
            return;
        }

        BigDecimal baseTotalRate = calculateBaseTotalRate(booking);
        TaxSummary taxSummary = calculateTaxSummary(booking);
        booking.setTotalRate(baseTotalRate.add(taxSummary.taxAmount));
    }

    private BigDecimal calculateBaseTotalRate(ReservationBookingRecord booking) {
        if (booking == null
                || booking.getRate() == null
                || booking.getNumberOfRooms() == null
                || booking.getArrivalDate() == null
                || booking.getDepartureDate() == null) {
            return BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(booking.getArrivalDate(), booking.getDepartureDate());
        if (nights <= 0) {
            return BigDecimal.ZERO;
        }

        return booking.getRate()
                .multiply(BigDecimal.valueOf(booking.getNumberOfRooms().longValue()))
                .multiply(BigDecimal.valueOf(nights));
    }

    private boolean isSameRoomType(String left, String right) {
        String normalizedLeft = normalizeValue(left);
        String normalizedRight = normalizeValue(right);

        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return false;
        }

        return normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft);
    }

    private String resolveRoomStatus(ReservationBookingRecord booking) {
        if (!StringUtils.hasText(booking.getPropertyId()) || !StringUtils.hasText(booking.getConfirmationNumber())) {
            return null;
        }

        LocalDate housekeepingBusinessDate = resolveHousekeepingBusinessDate(booking);
        if (housekeepingBusinessDate == null) {
            return null;
        }

        return housekeepingRoomStatusRepository
                .findByPropertyIdAndBusinessDateAndConfirmationNumber(
                        booking.getPropertyId(),
                        housekeepingBusinessDate,
                        booking.getConfirmationNumber()
                )
                .map(HousekeepingRoomStatusRecord::getRoomStatus)
                .orElse(null);
    }

    private LocalDate resolveBusinessDate(ReservationBookingRecord booking) {
        if (booking.getCheckInBusinessDate() != null) {
            return booking.getCheckInBusinessDate();
        }
        if (booking.getCreatedAt() != null) {
            return booking.getCreatedAt().toLocalDate();
        }
        return booking.getArrivalDate();
    }

    private LocalDate resolveHousekeepingBusinessDate(ReservationBookingRecord booking) {
        if (booking.getCheckInBusinessDate() != null) {
            return booking.getCheckInBusinessDate();
        }
        return booking.getArrivalDate();
    }

    private String preferredEmail(ReservationBookingRecord booking) {
        if (StringUtils.hasText(booking.getPersonalEmail())) {
            return booking.getPersonalEmail();
        }
        return booking.getOfficialEmail();
    }

    private String[] splitGuestName(String guestName) {
        if (!StringUtils.hasText(guestName)) {
            return new String[] {null, null};
        }

        String[] parts = guestName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : null;
        return new String[] {firstName, lastName};
    }

    private String formatTime(java.time.LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(TIME_FORMATTER);
    }

    private List<String> parseGuestRequests(String specialRequests) {
        if (!StringUtils.hasText(specialRequests)) {
            return List.of();
        }

        return Arrays.stream(specialRequests.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class TaxSummary {
        private final BigDecimal taxPercent;
        private final BigDecimal taxAmount;

        private TaxSummary(BigDecimal taxPercent, BigDecimal taxAmount) {
            this.taxPercent = taxPercent;
            this.taxAmount = taxAmount;
        }

        private static TaxSummary zero() {
            return new TaxSummary(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private void normalizeCreateRequest(ReservationBookingRequestDto request) {
        if (request == null) {
            return;
        }

        request.setSalutation(defaultIfBlank(request.getSalutation(), "Mr"));
        request.setReservationType(defaultIfBlank(request.getReservationType(), "GTD"));
        request.setCity(defaultIfBlank(request.getCity(), "UNKNOWN"));
        request.setCountry(defaultIfBlank(request.getCountry(), "UNKNOWN"));
        request.setZipCode(defaultIfBlank(request.getZipCode(), "000000"));

        if (!StringUtils.hasText(request.getPersonalEmail()) && StringUtils.hasText(request.getOfficialEmail())) {
            request.setPersonalEmail(request.getOfficialEmail().trim());
        }
        if (!StringUtils.hasText(request.getOfficialEmail()) && StringUtils.hasText(request.getPersonalEmail())) {
            request.setOfficialEmail(request.getPersonalEmail().trim());
        }

        if (!StringUtils.hasText(request.getGuestName()) && request.getGuestNames() != null && !request.getGuestNames().isEmpty()) {
            String first = request.getGuestNames().get(0);
            if (StringUtils.hasText(first)) {
                request.setGuestName(first.trim());
            }
        }

        if ((request.getGuestNames() == null || request.getGuestNames().isEmpty()) && StringUtils.hasText(request.getGuestName())) {
            request.setGuestNames(List.of(request.getGuestName().trim()));
        }

        if ((request.getRoomAssignments() == null || request.getRoomAssignments().isEmpty()) && StringUtils.hasText(request.getAssignedRoomNo())) {
            ReservationBookingRequestDto.RoomAssignmentDto assignment = new ReservationBookingRequestDto.RoomAssignmentDto();
            assignment.setRoomNumber(request.getAssignedRoomNo()); assignment.setRoomType(request.getRoomType()); assignment.setRoomTypeId(request.getRoomType());
            request.setRoomAssignments(List.of(assignment));
        }
        if (request.getRoomAssignments() != null && !request.getRoomAssignments().isEmpty()) request.setAssignedRoomNo(request.getRoomAssignments().get(0).getRoomNumber());

        if (request.getGuestNames() != null && request.getNumberOfRooms() != null && request.getNumberOfRooms() > 1) {
            List<String> normalizedGuestNames = new ArrayList<>(request.getGuestNames());
            while (normalizedGuestNames.size() < request.getNumberOfRooms()) {
                normalizedGuestNames.add(request.getGuestName());
            }
            request.setGuestNames(normalizedGuestNames);
        }

        if (request.getNoPost() == null) {
            request.setNoPost(Boolean.FALSE);
        }

        if (request.getVipTag() == null) {
            request.setVipTag(Boolean.FALSE);
        }

        if (request.getDnm() == null) {
            request.setDnm(Boolean.FALSE);
        }

        if (request.getDiscount() == null) {
            request.setDiscount(BigDecimal.ZERO);
        }

        if (request.getGuestBalance() == null) {
            request.setGuestBalance(BigDecimal.ZERO);
        }

        if (!StringUtils.hasText(request.getPaymentType())) {
            request.setPaymentType(PaymentTypes.FULL_PAYMENT);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private ReservationPaymentTransactionRecord buildPaymentTransaction(
            ReservationBookingRecord saved,
            ReservationBookingRequestDto request,
            BigDecimal amount,
            PaymentProcessingResult paymentResult
    ) {
        return ReservationPaymentTransactionRecord.builder()
                .bookingId(saved.getId())
                .confirmationNumber(saved.getConfirmationNumber())
                .propertyId(saved.getPropertyId())
                .paymentMode(request.getPayment())
                .amount(amount)
                .transactionStatus(paymentResult.getStatus())
                .transactionReference(paymentResult.getTransactionReference())
                .processorName(paymentResult.getProcessorName())
                .failureReason(paymentResult.getFailureReason())
                .processedAt(paymentResult.getProcessedAt() == null ? LocalDateTime.now() : paymentResult.getProcessedAt())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal calculatePayableAmount(ReservationBookingRequestDto request) {
        if (request.getRate() == null || request.getNumberOfRooms() == null || request.getArrivalDate() == null || request.getDepartureDate() == null) {
            return BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(request.getArrivalDate(), request.getDepartureDate());
        if (nights <= 0) {
            return BigDecimal.ZERO;
        }

        return request.getRate()
                .multiply(BigDecimal.valueOf(request.getNumberOfRooms().longValue()))
                .multiply(BigDecimal.valueOf(nights));
    }

    private InventoryDeductionRequest buildInventoryDeductionRequest(
            ReservationBookingRequestDto request,
            String confirmationNumber
    ) {
        return InventoryDeductionRequest.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .quantity(request.getNumberOfRooms())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .confirmationNumber(confirmationNumber)
                .build();
    }

    private InventorySyncRequest buildInventorySyncRequest(
            ReservationBookingRequestDto request,
            String confirmationNumber
    ) {
        return InventorySyncRequest.builder()
                .propertyId(request.getPropertyId())
                .roomType(request.getRoomType())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .confirmationNumber(confirmationNumber)
                .build();
    }

    private String generateConfirmationNumber(String propertyId) {
        for (int attempt = 0; attempt < CONFIRMATION_MAX_ATTEMPTS; attempt++) {
            String candidate = String.valueOf(ThreadLocalRandom.current()
                .nextInt(CONFIRMATION_MIN, CONFIRMATION_MAX_EXCLUSIVE));

            if (!reservationBookingRepository.existsByConfirmationNumber(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate unique confirmation number");
    }

    private void validatePropertyAndInventory(ReservationBookingRequestDto request) {
        if (!propertyWizardServiceProperties.isEnabled()) {
            return;
        }

        PropertyInventoryValidationResponse validation;
        try {
            validation = propertyInventoryPort.validateInventory(
                request.getPropertyId(),
                request.getRoomType(),
                request.getNumberOfRooms()
            );
        } catch (ExternalServiceException ex) {
            if (propertyWizardServiceProperties.isFailOpenOnValidationError()) {
                return;
            }
            throw ex;
        }

        if (!Boolean.TRUE.equals(validation.getPropertyExists())) {
            throw new BadRequestException("propertyId is invalid as per Property Wizard service");
        }

        if (!Boolean.TRUE.equals(validation.getRoomTypeAvailable())) {
            throw new BadRequestException("roomType is not available for selected property");
        }

        if (validation.getAvailableRooms() != null && request.getNumberOfRooms() > validation.getAvailableRooms()) {
            throw new BadRequestException("numberOfRooms exceeds available rooms for selected property and roomType");
        }
    }

    private void validateDates(LocalDate arrivalDate, LocalDate departureDate) {
        if (departureDate != null && arrivalDate != null && departureDate.isBefore(arrivalDate)) {
            throw new BadRequestException("departureDate must be on or after arrivalDate");
        }
    }

    private void validateRoomSelectionAndGuestNames(ReservationBookingRequestDto request) {
        if (request.getNumberOfRooms() == null || request.getNumberOfRooms() < 1 || request.getNumberOfRooms() > 9) {
            throw new BadRequestException("numberOfRooms must be between 1 and 9");
        }

        List<String> guestNames = request.getGuestNames();
        if (guestNames == null || guestNames.size() != request.getNumberOfRooms()) {
            throw new BadRequestException("guestNames count must match numberOfRooms");
        }

        boolean hasBlankGuestName = guestNames.stream().anyMatch(name -> !StringUtils.hasText(name));
        if (hasBlankGuestName) {
            throw new BadRequestException("guestNames must not contain blank values");
        }
        if (request.getRoomAssignments() != null
                && !request.getRoomAssignments().isEmpty()
                && request.getRoomAssignments().size() != request.getNumberOfRooms()) {
            throw new BadRequestException("roomAssignments count must match numberOfRooms when rooms are assigned");
        }
    }

    private void validateRoomAssignments(ReservationBookingRequestDto request) {
        if (request.getRoomAssignments() == null || request.getRoomAssignments().isEmpty()) {
            return;
        }
        LocalDate lastNight = request.getDepartureDate().minusDays(1);
        for (ReservationBookingRequestDto.RoomAssignmentDto assignment : request.getRoomAssignments()) {
            JsonNode calendar = housekeepingRoomCalendarClient.fetchCalendar(request.getPropertyId(), request.getArrivalDate(), lastNight, assignment.getRoomTypeId());
            JsonNode room = findCalendarRoom(calendar, assignment.getRoomNumber());
            if (room == null) throw new BadRequestException("Assigned room was not found: " + assignment.getRoomNumber());
            for (JsonNode day : room.path("days")) if (!day.path("sellable").asBoolean(false) || !"NOT_RESERVED".equalsIgnoreCase(day.path("reservationStatus").asText())) throw new BadRequestException("Assigned room is unavailable on " + day.path("date").asText());
        }
    }
    private JsonNode findCalendarRoom(JsonNode calendar, String roomNumber) { for (JsonNode type : calendar.path("roomTypes")) for (JsonNode room : type.path("rooms")) if (roomNumber.equalsIgnoreCase(room.path("roomNumber").asText())) return room; return null; }
    private void markRoomsAssigned(ReservationBookingRequestDto request, String confirmationNumber) {
        if (request.getRoomAssignments() == null || request.getRoomAssignments().isEmpty()) {
            return;
        }
        for (ReservationBookingRequestDto.RoomAssignmentDto assignment : request.getRoomAssignments())
            for (LocalDate date = request.getArrivalDate(); date.isBefore(request.getDepartureDate()); date = date.plusDays(1)) {
                com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.createObjectNode();
                body.put("propertyId", request.getPropertyId());
                body.put("businessDate", date.toString());
                body.put("confirmationNumber", confirmationNumber);
                body.put("frontOfficeStatus", "OCCUPIED");
                body.put("reservationStatus", "ARRIVAL");
                body.put("guestDisplayName", request.getGuestName());
                body.put("arrivalDate", request.getArrivalDate().toString());
                body.put("departureDate", request.getDepartureDate().toString());
                body.put("sourceModule", "RESERVATION");
                housekeepingRoomCalendarClient.markAssigned(assignment.getRoomNumber(), body);
            }
    }

    private void validateRequiredContactFields(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getOfficialEmail())) {
            throw new BadRequestException("officialEmail is required");
        }

        if (!StringUtils.hasText(request.getPersonalEmail())) {
            throw new BadRequestException("personalEmail is required");
        }
    }

    private void validateAndNormalizePaymentMode(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getPayment())) {
            throw new BadRequestException("payment is required");
        }

        String normalizedPaymentMode = PaymentModes.normalize(request.getPayment());

        if (!PaymentModes.isSupported(normalizedPaymentMode)) {
            throw new BadRequestException("payment must be one of CARD, CASH, UPI, NET_BANKING, WALLET");
        }

        request.setPayment(normalizedPaymentMode);
    }

    private void validateAndNormalizePaymentType(ReservationBookingRequestDto request) {
        if (!StringUtils.hasText(request.getPaymentType())) {
            throw new BadRequestException("paymentType is required");
        }

        String normalizedPaymentType = PaymentTypes.normalize(request.getPaymentType());
        if (!PaymentTypes.isSupported(normalizedPaymentType)) {
            throw new BadRequestException("paymentType must be one of ADVANCE, FULL_PAYMENT");
        }

        request.setPaymentType(normalizedPaymentType);
    }

    private void validatePhoneNumberFormat(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BadRequestException("phoneNumber is required");
        }

        if (!phoneNumber.matches("\\d{10}")) {
            throw new BadRequestException("phoneNumber must be exactly 10 digits");
        }
    }

    private void preserveSystemFields(ReservationBookingRecord existing, ReservationBookingRecord updated) {
        updated.setId(existing.getId());
        updated.setConfirmationNumber(existing.getConfirmationNumber());
        updated.setReservationStatus(existing.getReservationStatus());
        updated.setAssignedRoomNo(existing.getAssignedRoomNo());
        updated.setFloor(existing.getFloor());
        updated.setInventoryDeductedAt(existing.getInventoryDeductedAt());
        updated.setInventorySyncedAt(existing.getInventorySyncedAt());
        updated.setCheckInCompletedAt(existing.getCheckInCompletedAt());
        updated.setCheckInCompletedBy(existing.getCheckInCompletedBy());
        updated.setCheckInBusinessDate(existing.getCheckInBusinessDate());
        updated.setPayment(existing.getPayment());
        updated.setPaymentType(existing.getPaymentType());
        updated.setCreatedAt(existing.getCreatedAt());
    }
}
