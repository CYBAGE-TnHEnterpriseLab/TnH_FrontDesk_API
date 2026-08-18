package com.pms.reservation.service;

import com.pms.reservation.dto.CheckoutCompletionResponseDto;
import com.pms.reservation.dto.CheckoutRequestDto;

public interface ReservationCheckoutService {

    CheckoutCompletionResponseDto completeCheckout(String confirmationNumber, CheckoutRequestDto request);

    CheckoutCompletionResponseDto cancelCheckout(String confirmationNumber, CheckoutRequestDto request);
}
