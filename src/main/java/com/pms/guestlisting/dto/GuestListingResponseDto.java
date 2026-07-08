package com.pms.guestlisting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GuestListingResponseDto {
    String listingType;
    Long id;
    String propertyId;
    String status;
    Boolean dnm;
    Boolean msg;
    String salutation;
    String firstName;
    String lastName;
    String roomNo;
    String reservationType;
    String city;
    String rateCode;
    String ratePlan;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer roomNights;
    Integer nights;
    String roomStatus;
    String corporateCode;
    String roomType;
    String confirmationNumber;
    String company;
    String sharingStatus;
    String sharing;
    Integer floor;
    BigDecimal balance;
    String loyaltyMembershipStatus;
    String tier;
    String groupCode;
    String stayStatus;
}