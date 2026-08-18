package com.pms.reservation.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckInStep {

    GUEST_DETAILS("Guest Details", 1),
    ROOM_STAY("Room and Stay Details", 2),
    SIGNATURE("Guest Signature", 3),
    PAYMENT_VALIDATION("Payment Validation", 4),
    COMPLETE_CHECKIN("Complete Check-In", 5);

    private final String label;
    private final int sequence;
}
