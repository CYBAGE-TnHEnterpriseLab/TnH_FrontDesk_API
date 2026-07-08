package com.pms.reservation.dto;

import com.pms.reservation.constant.PaymentModes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationBookingRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    @NotBlank(message = "salutation is required")
    private String salutation;

    @NotNull(message = "vipTag is required")
    private Boolean vipTag = Boolean.FALSE;

    @NotBlank(message = "guestName is required")
    private String guestName;

    @NotEmpty(message = "guestNames is required")
    private List<@NotBlank(message = "guestNames must not contain blank values") String> guestNames;

    @NotBlank(message = "personalEmail is required")
    @Email(message = "personalEmail must be a valid email")
    private String personalEmail;

    @NotBlank(message = "officialEmail is required")
    @Email(message = "officialEmail must be a valid email")
    private String officialEmail;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "country is required")
    private String country;

    @NotBlank(message = "zipCode is required")
    private String zipCode;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "phoneNumber is invalid")
    private String phoneNumber;

    @NotBlank(message = "mobileNumber is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "mobileNumber is invalid")
    private String mobileNumber;

    private String loyaltyNumber;
    private String company;
    private String guestGroup;
    private String source;
    private String agent;

    @NotNull(message = "arrivalDate is required")
    private LocalDate arrivalDate;

    @NotNull(message = "departureDate is required")
    private LocalDate departureDate;

    @NotNull(message = "adultCount is required")
    @Min(value = 1, message = "adultCount must be >= 1")
    private Integer adultCount;

    @NotNull(message = "childCount is required")
    @Min(value = 0, message = "childCount must be >= 0")
    private Integer childCount;

    @NotBlank(message = "reservationType is required")
    @Pattern(regexp = "(?i)GTD|NON[\\s-]?GTD", message = "reservationType must be GTD or Non GTD")
    private String reservationType;

    @NotBlank(message = "roomType is required")
    private String roomType;

    @NotBlank(message = "rateCode is required")
    private String rateCode;

    @NotNull(message = "numberOfRooms is required")
    @Min(value = 1, message = "numberOfRooms must be >= 1")
    @Max(value = 9, message = "numberOfRooms must be <= 9")
    private Integer numberOfRooms;

    @NotNull(message = "rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "rate must be > 0")
    private BigDecimal rate;

    @Schema(description = "Reservation payment mode", allowableValues = {
            PaymentModes.CARD,
            PaymentModes.CASH,
            PaymentModes.UPI,
            PaymentModes.NET_BANKING,
            PaymentModes.WALLET
    })
    @NotBlank(message = "payment is required")
    @Pattern(
            regexp = PaymentModes.VALIDATION_PATTERN,
            message = "payment must be one of CARD, CASH, UPI, NET_BANKING, WALLET"
    )
    private String payment;

    @NotNull(message = "eta is required")
    private LocalTime eta;

    @NotNull(message = "checkOutTime is required")
    private LocalTime checkOutTime;

    @NotNull(message = "dnm is required")
    private Boolean dnm = Boolean.FALSE;

    @NotNull(message = "noPost is required")
    private Boolean noPost = Boolean.FALSE;

    @NotNull(message = "guestBalance is required")
    @DecimalMin(value = "0.0", message = "guestBalance must be >= 0")
    private BigDecimal guestBalance;

    private String specialRequests;

    @NotNull(message = "discount is required")
    @DecimalMin(value = "0.0", message = "discount must be >= 0")
    private BigDecimal discount;

    private String alertsMessages;
}
