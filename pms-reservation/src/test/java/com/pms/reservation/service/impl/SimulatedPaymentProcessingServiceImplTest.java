package com.pms.reservation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SimulatedPaymentProcessingServiceImplTest {

    private final SimulatedPaymentProcessingServiceImpl service = new SimulatedPaymentProcessingServiceImpl();

    @Test
    void processPaymentShouldReturnSuccessWithPayPrefixForNonCashMode() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPayment("CARD");

        PaymentProcessingResult result = service.processPayment(
                request,
                "PROP001-20260708120000000-123",
                new BigDecimal("5000.00")
        );

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTransactionReference()).startsWith("PAY-");
        assertThat(result.getProcessorName()).isEqualTo("SIMULATED_GATEWAY");
        assertThat(result.getFailureReason()).isNull();
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void processPaymentShouldReturnSuccessWithCshPrefixForCashMode() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPayment("cash");

        PaymentProcessingResult result = service.processPayment(
                request,
                "PROP001-20260708120000000-124",
                new BigDecimal("2500.00")
        );

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTransactionReference()).startsWith("CSH-");
        assertThat(result.getProcessorName()).isEqualTo("SIMULATED_GATEWAY");
        assertThat(result.getFailureReason()).isNull();
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void processPaymentShouldReturnFailureWhenAmountIsZeroOrNegative() {
        ReservationBookingRequestDto request = new ReservationBookingRequestDto();
        request.setPayment("UPI");

        PaymentProcessingResult result = service.processPayment(
                request,
                "PROP001-20260708120000000-125",
                BigDecimal.ZERO
        );

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getTransactionReference()).startsWith("FAIL-");
        assertThat(result.getProcessorName()).isEqualTo("SIMULATED_GATEWAY");
        assertThat(result.getFailureReason()).isEqualTo("amount must be greater than zero");
        assertThat(result.getProcessedAt()).isNotNull();
    }
}
