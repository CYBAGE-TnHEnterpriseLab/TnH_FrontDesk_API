package com.pms.reservation.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GuestContactDetailsDto {
    String personalEmail;
    String officialEmail;
    String phoneNumber;
    String mobileNumber;
    String city;
    String country;
    String zipCode;
}
