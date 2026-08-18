package com.pms.guestlisting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArrivalResponseDto {
    Long id;
    String propertyId;
    String status;
    String salutation;
    String firstName;
    String lastName;
    String roomNo;
    String reservationType;
    String city;
    String rateCode;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer roomNights;
    String roomStatus;
    String corporateCode;
    String roomType;
    String confirmationNumber;
    String company;
    String sharingStatus;
    Integer floor;
    BigDecimal balance;
    String loyaltyMembershipStatus;
}

