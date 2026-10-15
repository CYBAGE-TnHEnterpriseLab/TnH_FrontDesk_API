package com.pms.guest.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GuestDetailsResponse {
    String propertyId;
    List<GuestAssignmentDetails> guests;

    @Value
    @Builder
    public static class GuestAssignmentDetails {
        Long bookingId;
        String confirmationNumber;
        Long guestProfileId;
        Boolean isPrimary;
        GuestDetails guest;
        Membership membership;
    }

    @Value
    @Builder
    public static class GuestDetails {
        String guestId;
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
        java.time.LocalDate dateOfBirth;
        String gender;
        String companyName;
        Boolean vipStatus;
        String idType;
        String idNumber;
        String idDocumentPath;
    }

    @Value
    @Builder
    public static class Membership {
        boolean enrolled;
        String membershipNumber;
        String tier;
    }
}
