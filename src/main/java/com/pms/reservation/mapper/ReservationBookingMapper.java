package com.pms.reservation.mapper;

import com.pms.reservation.dto.ReservationBookingRequestDto;
import com.pms.reservation.dto.ReservationBookingResponseDto;
import com.pms.reservation.entity.ReservationBookingRecord;
import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationBookingMapper {

    public ReservationBookingRecord toEntity(ReservationBookingRequestDto request) {
        List<String> guestNames = sanitizeGuestNames(request.getGuestNames());
        return ReservationBookingRecord.builder()
                .propertyId(request.getPropertyId())
                .salutation(request.getSalutation())
                .vipTag(request.getVipTag())
            .guestName(primaryGuestName(request.getGuestName(), guestNames))
            .guestNamesEncoded(encodeGuestNames(guestNames))
                .personalEmail(request.getPersonalEmail())
                .officialEmail(request.getOfficialEmail())
                .city(request.getCity())
                .country(request.getCountry())
                .zipCode(request.getZipCode())
                .phoneNumber(request.getPhoneNumber())
                .mobileNumber(request.getMobileNumber())
                .loyaltyNumber(request.getLoyaltyNumber())
                .company(request.getCompany())
                .guestGroup(request.getGuestGroup())
                .source(request.getSource())
                .agent(request.getAgent())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .adultCount(request.getAdultCount())
                .childCount(request.getChildCount())
                .reservationType(request.getReservationType())
                .roomType(request.getRoomType())
                .rateCode(request.getRateCode())
                .numberOfRooms(request.getNumberOfRooms())
                .rate(request.getRate())
                .totalRate(calculateTotalRate(
                    request.getRate(),
                    request.getNumberOfRooms(),
                    request.getArrivalDate(),
                    request.getDepartureDate()
                ))
                .payment(request.getPayment())
                .paymentType(request.getPaymentType())
                .eta(request.getEta())
                .checkOutTime(request.getCheckOutTime())
                .dnm(request.getDnm())
                .noPost(request.getNoPost())
                .guestBalance(request.getGuestBalance())
                .specialRequests(request.getSpecialRequests())
                .discount(request.getDiscount())
                .alertsMessages(request.getAlertsMessages())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ReservationBookingResponseDto toResponse(ReservationBookingRecord saved) {
        return toResponse(saved, null);
        }

        public ReservationBookingResponseDto toResponse(
            ReservationBookingRecord saved,
            ReservationPaymentTransactionRecord transaction
        ) {
        return ReservationBookingResponseDto.builder()
                .bookingId(saved.getId())
                .confirmationNumber(saved.getConfirmationNumber())
                .reservationStatus(saved.getReservationStatus())
                .propertyId(saved.getPropertyId())
                .salutation(saved.getSalutation())
                .vipTag(saved.getVipTag())
                .guestName(saved.getGuestName())
                .guestNames(decodeGuestNames(saved.getGuestNamesEncoded()))
                .personalEmail(saved.getPersonalEmail())
                .officialEmail(saved.getOfficialEmail())
                .city(saved.getCity())
                .country(saved.getCountry())
                .zipCode(saved.getZipCode())
                .phoneNumber(saved.getPhoneNumber())
                .mobileNumber(saved.getMobileNumber())
                .loyaltyNumber(saved.getLoyaltyNumber())
                .company(saved.getCompany())
                .guestGroup(saved.getGuestGroup())
                .source(saved.getSource())
                .agent(saved.getAgent())
                .arrivalDate(saved.getArrivalDate())
                .departureDate(saved.getDepartureDate())
                .adultCount(saved.getAdultCount())
                .childCount(saved.getChildCount())
                .reservationType(saved.getReservationType())
                .roomType(saved.getRoomType())
                .assignedRoomNo(saved.getAssignedRoomNo())
                .rateCode(saved.getRateCode())
                .numberOfRooms(saved.getNumberOfRooms())
                .rate(saved.getRate())
                .totalRate(saved.getTotalRate())
                .payment(saved.getPayment())
                .paymentType(saved.getPaymentType())
                .eta(saved.getEta())
                .checkOutTime(saved.getCheckOutTime())
                .dnm(saved.getDnm())
                .noPost(saved.getNoPost())
                .guestBalance(saved.getGuestBalance())
                .specialRequests(saved.getSpecialRequests())
                .discount(saved.getDiscount())
                .alertsMessages(saved.getAlertsMessages())
                .inventoryDeductedAt(saved.getInventoryDeductedAt())
                .inventorySyncedAt(saved.getInventorySyncedAt())
                .checkInBusinessDate(saved.getCheckInBusinessDate())
                .checkInCompletedAt(saved.getCheckInCompletedAt())
                .checkInCompletedBy(saved.getCheckInCompletedBy())
                .paymentTransactionStatus(transaction == null ? null : transaction.getTransactionStatus())
                .paymentTransactionReference(transaction == null ? null : transaction.getTransactionReference())
                .paymentProcessorName(transaction == null ? null : transaction.getProcessorName())
                .paymentProcessedAt(transaction == null ? null : transaction.getProcessedAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private String primaryGuestName(String guestName, List<String> guestNames) {
        if (StringUtils.hasText(guestName)) {
            return guestName.trim();
        }
        return guestNames.isEmpty() ? guestName : guestNames.get(0);
    }

    private List<String> sanitizeGuestNames(List<String> guestNames) {
        if (guestNames == null) {
            return Collections.emptyList();
        }
        return guestNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String encodeGuestNames(List<String> guestNames) {
        if (guestNames == null || guestNames.isEmpty()) {
            return "";
        }
        return guestNames.stream()
                .map(name -> Base64.getEncoder().encodeToString(name.getBytes()))
                .collect(Collectors.joining("."));
    }

    private List<String> decodeGuestNames(String guestNamesEncoded) {
        if (!StringUtils.hasText(guestNamesEncoded)) {
            return Collections.emptyList();
        }

        String[] parts = guestNamesEncoded.split("\\.");
        List<String> guestNames = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                guestNames.add(new String(Base64.getDecoder().decode(part)));
            }
        }
        return guestNames;
    }

    private java.math.BigDecimal calculateTotalRate(
            java.math.BigDecimal rate,
            Integer numberOfRooms,
            LocalDate arrivalDate,
            LocalDate departureDate
    ) {
        if (rate == null || numberOfRooms == null || arrivalDate == null || departureDate == null) {
            return java.math.BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(arrivalDate, departureDate);
        if (nights <= 0) {
            return java.math.BigDecimal.ZERO;
        }

        return rate
                .multiply(java.math.BigDecimal.valueOf(numberOfRooms.longValue()))
                .multiply(java.math.BigDecimal.valueOf(nights));
    }
}
