package com.pms.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationGuestRequestDto {

    private Long guestProfileId;

    @NotNull(message = "isPrimary is required")
    private Boolean isPrimary;

    private Boolean enrollGuest;

    @Size(max = 20)
    private String salutation;

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Email(message = "personalEmail must be a valid email")
    @Size(max = 160)
    private String personalEmail;

    @Email(message = "officialEmail must be a valid email")
    @Size(max = 160)
    private String officialEmail;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 80)
    private String city;

    @Size(max = 80)
    private String state;

    @Size(max = 80)
    private String country;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 80)
    private String nationality;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String gender;

    @Size(max = 120)
    private String companyName;

    private Boolean vipStatus;

    @Size(max = 40)
    private String idType;

    @Size(max = 80)
    private String idNumber;

    @Size(max = 500)
    private String idDocumentPath;

    @Size(max = 40)
    private String loyaltyMembershipNumber;
}
