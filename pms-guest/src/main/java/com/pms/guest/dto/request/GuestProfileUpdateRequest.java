package com.pms.guest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestProfileUpdateRequest {

    @Size(max = 20)
    private String salutation;

    @Size(max = 80, message = "firstName must not exceed 80 characters")
    private String firstName;

    @Size(max = 80, message = "lastName must not exceed 80 characters")
    private String lastName;

    @Email(message = "personalEmail must be a valid email")
    @Size(max = 160, message = "personalEmail must not exceed 160 characters")
    private String personalEmail;

    @Email(message = "officialEmail must be a valid email")
    @Size(max = 160, message = "officialEmail must not exceed 160 characters")
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

    @Size(max = 40)
    private String loyaltyTier;
}
