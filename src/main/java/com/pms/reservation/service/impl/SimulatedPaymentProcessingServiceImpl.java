package com.pms.reservation.service.impl;

import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.service.PaymentProcessingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class SimulatedPaymentProcessingServiceImpl implements PaymentProcessingService {

    private static final DateTimeFormatter TXN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public PaymentProcessingResult processPayment(
            ReservationBookingRequestDto request,
            String confirmationNumber,
            BigDecimal amount
    ) {
        LocalDateTime processedAt = LocalDateTime.now();
        String paymentMode = request.getPayment() == null ? "UNKNOWN" : request.getPayment().trim().toUpperCase();

        if (amount == null || amount.signum() <= 0) {
            return PaymentProcessingResult.builder()
                    .status("FAILED")
                    .transactionReference(generateTransactionReference("FAIL"))
                    .processorName("SIMULATED_GATEWAY")
                    .failureReason("amount must be greater than zero")
                    .processedAt(processedAt)
                    .build();
        }

        String referencePrefix = "CASH".equals(paymentMode) ? "CSH" : "PAY";
        return PaymentProcessingResult.builder()
                .status("SUCCESS")
                .transactionReference(generateTransactionReference(referencePrefix))
                .processorName("SIMULATED_GATEWAY")
                .failureReason(null)
                .processedAt(processedAt)
                .build();
    }

    private String generateTransactionReference(String prefix) {
        int randomSuffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return prefix + "-" + LocalDateTime.now().format(TXN_TIME_FORMATTER) + "-" + randomSuffix;
    }
}
