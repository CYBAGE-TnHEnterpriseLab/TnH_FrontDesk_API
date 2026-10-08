package com.pms.guest.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestLookupRequest {

    @NotBlank(message = "propertyId is required")
    @Size(max = 36, message = "propertyId must not exceed 36 characters")
    private String propertyId;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 20)
    private String mobileNumber;

    @Email(message = "personalEmail must be a valid email")
    @Size(max = 160)
    private String personalEmail;

    @Email(message = "officialEmail must be a valid email")
    @Size(max = 160)
    private String officialEmail;

    @Size(max = 40)
    private String loyaltyNumber;

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @AssertTrue(message = "At least one contact or loyalty lookup field is required")
    public boolean isLookupIdentifierProvided() {
        return hasText(phoneNumber)
                || hasText(mobileNumber)
                || hasText(personalEmail)
                || hasText(officialEmail)
                || hasText(loyaltyNumber);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
