package com.pms.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationBookingResponseDto {
    Long bookingId;
    String confirmationNumber;
    String reservationStatus;
    String propertyId;
    String salutation;
    Boolean vipTag;
    String guestName;
    List<String> guestNames;
    String personalEmail;
    String officialEmail;
    String city;
    String country;
    String zipCode;
    String phoneNumber;
    String mobileNumber;
    String loyaltyNumber;
    String company;
    String guestGroup;
    String source;
    String agent;
    LocalDate arrivalDate;
    LocalDate departureDate;
    Integer adultCount;
    Integer childCount;
    String reservationType;
    String roomType;
    String rateCode;
    Integer numberOfRooms;
    BigDecimal rate;
    BigDecimal totalRate;
    String payment;
    LocalTime eta;
    LocalTime checkOutTime;
    Boolean dnm;
    Boolean noPost;
    BigDecimal guestBalance;
    String specialRequests;
    BigDecimal discount;
    String alertsMessages;
    LocalDateTime inventoryDeductedAt;
    LocalDateTime inventorySyncedAt;
    String paymentTransactionStatus;
    String paymentTransactionReference;
    String paymentProcessorName;
    LocalDateTime paymentProcessedAt;
    LocalDateTime createdAt;
}
