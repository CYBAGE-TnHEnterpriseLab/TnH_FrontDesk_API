package com.hotel.pms.frontdesk.guestlisting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrivalSearchRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotNull(message = "businessDate is required")
    private LocalDate businessDate;

    private String search;
    private String status;
    private String reservationType;
    private String city;
    private String roomStatus;
    private String corporateCode;
    private String roomType;
    private String company;
    private String sharingStatus;
    private String loyaltyMembershipStatus;

    @Min(value = 0, message = "page must be >= 0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be >= 1")
    @Max(value = 100, message = "size must be <= 100")
    private Integer size = 20;

    private String sortBy = "checkInDate";

    @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc")
    private String sortDir = "asc";
}
