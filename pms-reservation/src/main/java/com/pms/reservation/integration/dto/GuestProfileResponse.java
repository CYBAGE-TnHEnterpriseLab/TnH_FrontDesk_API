package com.pms.reservation.integration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GuestProfileResponse {
    Long id;
    String guestId;
    String propertyId;
    String salutation;
    String firstName;
    String lastName;
    String personalEmail;
    String officialEmail;
    String phoneNumber;
    String mobileNumber;
    String address;
    String city;
    String state;
    String country;
    String postalCode;
    String nationality;
    LocalDate dateOfBirth;
    String gender;
    String companyName;
    Boolean vipStatus;
    String idType;
    String idNumber;
    String idDocumentPath;
    String loyaltyMembershipNumber;
    String loyaltyTier;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
