package com.pms.reservation.service.impl;

import com.pms.guestlisting.exception.BadRequestException;
import com.pms.reservation.config.PropertyWizardServiceProperties;
import com.pms.reservation.constant.PaymentModes;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private static final DateTimeFormatter CONFIRMATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ReservationBookingRepository reservationBookingRepository;
    private final ReservationPaymentTransactionRepository reservationPaymentTransactionRepository;
    private final PropertyInventoryPort propertyInventoryPort;
    private final PropertyWizardServiceProperties propertyWizardServiceProperties;
    private final ReservationBookingMapper reservationBookingMapper;
    private final PaymentProcessingService paymentProcessingService;

    @Override
    @Transactional
    public ReservationBookingResponseDto createBooking(ReservationBookingRequestDto request) {
        validateDates(request.getArrivalDate(), request.getDepartureDate());
        validateRoomSelectionAndGuestNames(request);
        validateAndNormalizePaymentMode(request);
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
            propertyInventoryPort.deductInventory(buildInventoryDeductionRequest(request, confirmationNumber));
            inventoryDeductedAt = LocalDateTime.now();

            propertyInventoryPort.syncInventory(buildInventorySyncRequest(request, confirmationNumber));
            inventorySyncedAt = LocalDateTime.now();
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
        String propertyToken = propertyId == null
                ? "RES"
                : propertyId.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);

        if (propertyToken.isBlank()) {
            propertyToken = "RES";
        }

        int randomSuffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return propertyToken
                + "-"
                + LocalDateTime.now().format(CONFIRMATION_TIME_FORMATTER)
                + "-"
                + randomSuffix;
    }

    private void validatePropertyAndInventory(ReservationBookingRequestDto request) {
        if (!propertyWizardServiceProperties.isEnabled()) {
            return;
        }

        PropertyInventoryValidationResponse validation = propertyInventoryPort.validateInventory(
                request.getPropertyId(),
                request.getRoomType(),
                request.getNumberOfRooms()
        );

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
}