package com.hotel.pms.frontdesk.guestlisting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationArrivalDto {
    private String status;
    private String salutation;
    private String firstName;
    private String lastName;
    private String roomNo;
    private String reservationType;
    private String city;
    private String rateCode;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomNights;
    private String roomStatus;
    private String corporateCode;
    private String roomType;
    private String confirmationNumber;
    private String company;
    private String sharingStatus;
    private Integer floor;
    private BigDecimal balance;
    private String loyaltyMembershipStatus;
}
