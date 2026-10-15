package com.folio.billing.dto;

import java.math.BigDecimal;

public record GuestDetail(
        String guestName,
        int guestAge,
        String guestPhoneNumber,
        String guestEmailId,
        String guestAddress,
        BigDecimal dueAmount
) {
}
