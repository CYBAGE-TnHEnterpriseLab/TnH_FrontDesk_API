package com.pms.reservation.integration.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GuestLookupRequest {
    String propertyId;
    String phoneNumber;
    String mobileNumber;
    String personalEmail;
    String officialEmail;
    String loyaltyNumber;
    String firstName;
    String lastName;
}
