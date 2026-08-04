package com.pms.reservation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pms.reservation.constant.PaymentModes;
import com.pms.reservation.constant.PaymentTypes;
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
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class ReservationBookingRequestDto {

    @NotBlank(message = "propertyId is required")
    private String propertyId;

    private String salutation;

    @NotNull(message = "vipTag is required")
    private Boolean vipTag = Boolean.FALSE;

    @NotBlank(message = "guestName is required")
    private String guestName;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String firstName;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String lastName;

    private List<@NotBlank(message = "guestNames must not contain blank values") String> guestNames;

    @Email(message = "personalEmail must be a valid email")
    private String personalEmail;

    @Email(message = "officialEmail must be a valid email")
        @JsonAlias({"email", "guestEmail"})
    private String officialEmail;

    private String city;

    private String country;

    private String zipCode;

    @NotBlank(message = "phoneNumber is required")
        @JsonAlias({"phone", "contactNumber"})
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "phoneNumber is invalid")
    private String phoneNumber;

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

    @Pattern(regexp = "(?i)GTD|NON[\\s-]?GTD", message = "reservationType must be GTD or Non GTD")
    private String reservationType;

    @NotBlank(message = "roomType is required")
    private String roomType;

    @NotBlank(message = "rateCode is required")
        @JsonAlias({"ratePlan", "ratePlanCode", "planCode"})
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

    @Schema(description = "Reservation payment type", allowableValues = {
            PaymentTypes.ADVANCE,
            PaymentTypes.FULL_PAYMENT
    })
    @Pattern(
            regexp = PaymentTypes.VALIDATION_PATTERN,
            message = "paymentType must be one of ADVANCE, FULL_PAYMENT"
    )
    private String paymentType;

    @JsonDeserialize(using = LenientLocalTimeDeserializer.class)
    @NotNull(message = "eta is required")
    private LocalTime eta;

    @JsonDeserialize(using = LenientLocalTimeDeserializer.class)
    @NotNull(message = "checkOutTime is required")
    private LocalTime checkOutTime;

    @NotNull(message = "dnm is required")
    private Boolean dnm = Boolean.FALSE;

    private Boolean noPost = Boolean.FALSE;

    @NotNull(message = "guestBalance is required")
    @DecimalMin(value = "0.0", message = "guestBalance must be >= 0")
    private BigDecimal guestBalance;

    private String specialRequests;

    @NotNull(message = "discount is required")
    @DecimalMin(value = "0.0", message = "discount must be >= 0")
    private BigDecimal discount;

    private String alertsMessages;

        @JsonIgnore
        private boolean guestNameExplicitlyProvided;

        public void setGuestName(String guestName) {
                this.guestName = guestName;
                this.guestNameExplicitlyProvided = StringUtils.hasText(guestName);
        }

        public void setFirstName(String firstName) {
                this.firstName = firstName;
                populateGuestNameFromNameParts();
        }

        public void setLastName(String lastName) {
                this.lastName = lastName;
                populateGuestNameFromNameParts();
        }

        public void setPaymentType(String paymentType) {
                if (!StringUtils.hasText(paymentType)) {
                        this.paymentType = paymentType;
                        return;
                }

                String normalized = paymentType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
                if ("SELECT_PAYMENT_TYPE".equals(normalized) || "SELECT".equals(normalized)) {
                        this.paymentType = null;
                        return;
                }

                this.paymentType = paymentType;
        }

        public void setGuestBalance(BigDecimal guestBalance) {
                if (guestBalance == null) {
                        this.guestBalance = null;
                        return;
                }

                this.guestBalance = guestBalance.signum() < 0 ? guestBalance.abs() : guestBalance;
        }

        private void populateGuestNameFromNameParts() {
                if (guestNameExplicitlyProvided) {
                        return;
                }

                String first = StringUtils.hasText(this.firstName) ? this.firstName.trim() : "";
                String last = StringUtils.hasText(this.lastName) ? this.lastName.trim() : "";
                String combined = (first + " " + last).trim();
                if (StringUtils.hasText(combined)) {
                        this.guestName = combined;
                        return;
                }

                if (StringUtils.hasText(first)) {
                        this.guestName = first;
                        return;
                }

                if (StringUtils.hasText(last)) {
                        this.guestName = last;
                }
        }
}
