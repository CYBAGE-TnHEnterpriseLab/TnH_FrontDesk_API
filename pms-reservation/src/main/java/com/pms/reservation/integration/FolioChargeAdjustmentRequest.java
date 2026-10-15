package com.pms.reservation.integration;

import java.math.BigDecimal;

public record FolioChargeAdjustmentRequest(
        String confirmationNumber,
        String originalReferenceNumber,
        ChargeAdjustmentType adjustmentType,
        BigDecimal amount,
        String reason,
        String userId,
        Long bookingId
) {
    public enum ChargeAdjustmentType {
        INCREASE,
        DECREASE
    }
}
