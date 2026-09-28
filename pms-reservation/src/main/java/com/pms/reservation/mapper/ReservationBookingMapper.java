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
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .zipCode(request.getZipCode())
                .phoneNumber(request.getPhoneNumber())
                .mobileNumber(request.getPhoneNumber())
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
                .blockCode(request.getBlockCode())
                .roomType(request.getRoomType())
                .assignedRoomNo(request.getAssignedRoomNo())
                .floor(request.getFloor())
                .rateCode(request.getRateCode())
                .numberOfRooms(request.getNumberOfRooms())
                .rate(request.getRate())
                .taxPercent(request.getTaxPercent())
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
                .address(saved.getAddress())
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
                .floor(saved.getFloor())
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

    String primaryGuestName(String guestName, List<String> guestNames) {
        if (StringUtils.hasText(guestName)) {
            return guestName.trim();
        }
        return guestNames.isEmpty() ? guestName : guestNames.get(0);
    }

    List<String> sanitizeGuestNames(List<String> guestNames) {
        if (guestNames == null) {
            return Collections.emptyList();
        }
        return guestNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    String encodeGuestNames(List<String> guestNames) {
        if (guestNames == null || guestNames.isEmpty()) {
            return "";
        }
        return guestNames.stream()
                .map(name -> Base64.getEncoder().encodeToString(name.getBytes()))
                .collect(Collectors.joining("."));
    }

    List<String> decodeGuestNames(String guestNamesEncoded) {
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

    java.math.BigDecimal calculateTotalRate(
            java.math.BigDecimal rate,
            Integer numberOfRooms,
            LocalDate arrivalDate,
            LocalDate departureDate
    ) {
        if (rate == null || numberOfRooms == null || arrivalDate == null || departureDate == null) {
            return java.math.BigDecimal.ZERO;
        }

        long nights = ChronoUnit.DAYS.between(arrivalDate, departureDate);
        if (nights < 0) {
            return java.math.BigDecimal.ZERO;
        }
        if (nights == 0) {
            nights = 1;
        }

        return rate
                .multiply(java.math.BigDecimal.valueOf(numberOfRooms.longValue()))
                .multiply(java.math.BigDecimal.valueOf(nights));
    }

    public ReservationBookingRecord mergeEntity(ReservationBookingRecord existing, ReservationBookingRequestDto request) {
        if (request.getSalutation() != null) existing.setSalutation(request.getSalutation());
        if (request.getVipTag() != null) existing.setVipTag(request.getVipTag());
        if (request.getGuestName() != null) existing.setGuestName(request.getGuestName());
        if (request.getPersonalEmail() != null) existing.setPersonalEmail(request.getPersonalEmail());
        if (request.getOfficialEmail() != null) existing.setOfficialEmail(request.getOfficialEmail());
        if (request.getAddress() != null) existing.setAddress(request.getAddress());
        if (request.getCity() != null) existing.setCity(request.getCity());
        if (request.getCountry() != null) existing.setCountry(request.getCountry());
        if (request.getZipCode() != null) existing.setZipCode(request.getZipCode());
        if (request.getPhoneNumber() != null) {
            existing.setPhoneNumber(request.getPhoneNumber());
            existing.setMobileNumber(request.getPhoneNumber());
        }
        if (request.getLoyaltyNumber() != null) existing.setLoyaltyNumber(request.getLoyaltyNumber());
        if (request.getCompany() != null) existing.setCompany(request.getCompany());
        if (request.getGuestGroup() != null) existing.setGuestGroup(request.getGuestGroup());
        if (request.getSource() != null) existing.setSource(request.getSource());
        if (request.getAgent() != null) existing.setAgent(request.getAgent());
        if (request.getArrivalDate() != null) existing.setArrivalDate(request.getArrivalDate());
        if (request.getDepartureDate() != null) existing.setDepartureDate(request.getDepartureDate());
        if (request.getAdultCount() != null) existing.setAdultCount(request.getAdultCount());
        if (request.getChildCount() != null) existing.setChildCount(request.getChildCount());
        if (request.getReservationType() != null) existing.setReservationType(request.getReservationType());
        if (request.getBlockCode() != null) existing.setBlockCode(request.getBlockCode());
        if (request.getRoomType() != null) existing.setRoomType(request.getRoomType());
        if (request.getAssignedRoomNo() != null) existing.setAssignedRoomNo(request.getAssignedRoomNo().trim());
        if (request.getFloor() != null) existing.setFloor(request.getFloor());
        if (request.getRateCode() != null) existing.setRateCode(request.getRateCode());
        if (request.getNumberOfRooms() != null) existing.setNumberOfRooms(request.getNumberOfRooms());
        if (request.getRate() != null) existing.setRate(request.getRate());
        if (request.getTaxPercent() != null) existing.setTaxPercent(request.getTaxPercent());
        if (request.getPayment() != null) existing.setPayment(request.getPayment());
        if (request.getPaymentType() != null) existing.setPaymentType(request.getPaymentType());
        if (request.getEta() != null) existing.setEta(request.getEta());
        if (request.getCheckOutTime() != null) existing.setCheckOutTime(request.getCheckOutTime());
        if (request.getDnm() != null) existing.setDnm(request.getDnm());
        if (request.getNoPost() != null) existing.setNoPost(request.getNoPost());
        if (request.getGuestBalance() != null) existing.setGuestBalance(request.getGuestBalance());
        if (request.getSpecialRequests() != null) existing.setSpecialRequests(request.getSpecialRequests());
        if (request.getDiscount() != null) existing.setDiscount(request.getDiscount());
        if (request.getAlertsMessages() != null) existing.setAlertsMessages(request.getAlertsMessages());

        existing.setTotalRate(calculateTotalRate(
                existing.getRate(),
                existing.getNumberOfRooms(),
                existing.getArrivalDate(),
                existing.getDepartureDate()
        ));

        List<String> guestNames = sanitizeGuestNames(request.getGuestNames());
        if (!guestNames.isEmpty() || request.getGuestName() != null || request.getFirstName() != null || request.getLastName() != null) {
            existing.setGuestName(primaryGuestName(request.getGuestName(), guestNames));
            existing.setGuestNamesEncoded(encodeGuestNames(guestNames));
        }

        return existing;
    }
}


