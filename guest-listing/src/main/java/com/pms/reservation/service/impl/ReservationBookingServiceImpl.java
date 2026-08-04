package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.guestlisting.exception.ExternalServiceException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.constant.PaymentTypes;
import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import com.pms.reservation.integration.PropertyInventoryPort;
import com.pms.reservation.integration.dto.InventoryDeductionRequest;
import com.pms.reservation.integration.dto.InventorySyncRequest;
import com.pms.reservation.integration.dto.PropertyInventoryValidationResponse;
import com.pms.reservation.mapper.ReservationBookingMapper;
import com.pms.reservation.repository.ReservationBookingRepository;
import com.pms.reservation.repository.ReservationPaymentTransactionRepository;
import com.pms.reservation.service.PaymentProcessingService;
import com.pms.reservation.service.ReservationBookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationBookingServiceImpl implements ReservationBookingService {

    private static final String RESERVATION_STATUS_CONFIRMED = "CONFIRMED";
    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final int CONFIRMATION_MIN = 1000000000;
    private static final int CONFIRMATION_MAX_EXCLUSIVE = 2000000000;
    private static final int CONFIRMATION_MAX_ATTEMPTS = 50;

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationPaymentTransactionRepository reservationPaymentTransactionRepository;
    private final PropertyInventoryPort propertyInventoryPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationBookingMapper reservationBookingMapper;
    private final PaymentProcessingService paymentProcessingService;

    @Override
    @Transactional
    public ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request) {
        normalizeCreateRequest(request);
        validateDates(request.getArrivalDate(), request.getDepartureDate());
        validateRequiredContactFields(request);
        validateRoomSelectionAndGuestNames(request);
        validateAndNormalizePaymentMode(request);
        validateAndNormalizePaymentType(request);
        String confirmationNumber = generateConfirmationNumber(request.getPropertyId());

        LocalDateTime inventoryDeductedAt = null;
        LocalDateTime inventorySyncedAt = null;

        if (propertyWizardServiceProperties.isEnabled()) {
            validatePropertyAndInventory(request);
        }

        BigDecimal payableAmount = calculatePayableAmount(request);
        PaymentProcessingResult paymentResult = paymentProcessingService.processPayment(request, confirmationNumber, payableAmount);
        if (!PAYMENT_STATUS_SUCCESS.equalsIgnoreCase(paymentResult.getStatus())) {
            String failureReason = paymentResult.getFailureReason() == null
                    ? "payment processing failed"
                    : paymentResult.getFailureReason();
            throw new BadRequestException("payment processing failed: " + failureReason);
        }

        if (propertyWizardServiceProperties.isEnabled()) {
            try {
                propertyInventoryPort.deductInventory(buildInventoryDeductionRequest(request, confirmationNumber));
                inventoryDeductedAt = LocalDateTime.now();

                propertyInventoryPort.syncInventory(buildInventorySyncRequest(request, confirmationNumber));
                inventorySyncedAt = LocalDateTime.now();
            } catch (ExternalServiceException ex) {
                if (!propertyWizardServiceProperties.isFailOpenOnWriteError()) {
                    throw ex;
                }
            }
        }

        ReservationBookingRecord entity = reservationBookingMapper.toEntity(request);
        entity.setConfirmationNumber(confirmationNumber);
        entity.setReservationStatus(RESERVATION_STATUS_CONFIRMED);
        entity.setInventoryDeductedAt(inventoryDeductedAt);
        entity.setInventorySyncedAt(inventorySyncedAt);

        ReservationBookingRecord saved = reservationBookingRepository.save(entity);
        ReservationPaymentTransactionRecord savedPaymentTransaction = reservationPaymentTransactionRepository
            .save(buildPaymentTransaction(saved, request, payableAmount, paymentResult));
        return reservationBookingMapper.toResponse(saved, savedPaymentTransaction);
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

        if (!StringUtils.hasText(request.getMobileNumber()) && StringUtils.hasText(request.getPhoneNumber())) {
            request.setMobileNumber(request.getPhoneNumber().trim());
        }

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
}