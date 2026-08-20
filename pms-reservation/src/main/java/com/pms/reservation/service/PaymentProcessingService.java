package com.pms.reservation.service;

import com.pms.reservation.dto.PaymentProcessingResult;
import com.pms.reservation.dto.ReservationBookingRequestDto;
import java.math.BigDecimal;

public interface PaymentProcessingService {

    PaymentProcessingResult processPayment(
            ReservationBookingRequestDto request,
            String confirmationNumber,
            BigDecimal amount
    );
}
