package com.pms.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReservationViewResponseDto {
    String reservationId;
    String confirmationNumber;
    String status;
    OffsetDateTime createdAt;
    String propertyId;
    LocalDate businessDate;
    Boolean vipTag;
    Boolean dnm;
    GuestDto guest;
    List<AdditionalGuestDto> additionalGuests;
    StayDto stay;
    RoomDto room;
    BookingDto booking;
    PricingDto pricing;
    CommentsDto comments;
    ActionsDto actions;

    @Value
    @Builder
    public static class GuestDto {
        String salutation;
        String firstName;
        String lastName;
        String phoneNumber;
        String email;
        String address;
        String city;
        String country;
        String zipCode;
        String loyaltyNumber;
    }

    @Value
    @Builder
    public static class AdditionalGuestDto {
        String name;
        String phoneNumber;
        String email;
    }

    @Value
    @Builder
    public static class StayDto {
        LocalDate checkInDate;
        LocalDate checkOutDate;
        Integer nights;
        Integer rooms;
        Integer adults;
        Integer children;
        List<Integer> childAges;
        String checkInTime;
        String checkOutTime;
    }

    @Value
    @Builder
    public static class RoomDto {
        String roomNo;
        String roomType;
        String floor;
        String roomStatus;
    }

    @Value
    @Builder
    public static class BookingDto {
        String groupCode;
        String company;
        String blockCode;
        String source;
        String reservationType;
        String rateCode;
    }

    @Value
    @Builder
    public static class PricingDto {
        String currency;
        BigDecimal roomRate;
        BigDecimal taxPercent;
        BigDecimal taxAmount;
        BigDecimal totalRate;
        BigDecimal guestBalance;
        BigDecimal discount;
    }

    @Value
    @Builder
    public static class CommentsDto {
        List<String> guestRequests;
        String billingComments;
    }

    @Value
    @Builder
    public static class ActionsDto {
        Boolean canEdit;
        Boolean canCheckIn;
        Boolean canCheckOut;
        Boolean canCancel;
    }
}
