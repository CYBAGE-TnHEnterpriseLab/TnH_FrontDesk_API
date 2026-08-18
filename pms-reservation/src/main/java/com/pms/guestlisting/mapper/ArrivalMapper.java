package com.pms.guestlisting.mapper;

import com.pms.guestlisting.dto.ArrivalResponseDto;
import com.pms.guestlisting.dto.ReservationArrivalDto;
import com.pms.guestlisting.entity.ArrivalRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ArrivalMapper {

    public ArrivalRecord toEntity(ReservationArrivalDto source, String propertyId, LocalDate businessDate) {
        return ArrivalRecord.builder()
                .businessDate(businessDate)
                .propertyId(propertyId)
                .status(source.getStatus())
                .salutation(source.getSalutation())
                .firstName(source.getFirstName())
                .lastName(source.getLastName())
                .roomNo(source.getRoomNo())
                .reservationType(source.getReservationType())
                .city(source.getCity())
                .rateCode(source.getRateCode())
                .checkInDate(source.getCheckInDate())
                .checkOutDate(source.getCheckOutDate())
                .roomNights(resolveRoomNights(source))
                .roomStatus(source.getRoomStatus())
                .corporateCode(source.getCorporateCode())
                .roomType(source.getRoomType())
                .confirmationNumber(source.getConfirmationNumber())
                .company(source.getCompany())
                .sharingStatus(source.getSharingStatus())
                .floor(source.getFloor())
                .balance(source.getBalance())
                .loyaltyMembershipStatus(source.getLoyaltyMembershipStatus())
                .sourceLastSyncedAt(LocalDateTime.now())
                .build();
    }

    public void updateEntity(ArrivalRecord target, ReservationArrivalDto source) {
        target.setStatus(source.getStatus());
        target.setSalutation(source.getSalutation());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setRoomNo(source.getRoomNo());
        target.setReservationType(source.getReservationType());
        target.setCity(source.getCity());
        target.setRateCode(source.getRateCode());
        target.setCheckInDate(source.getCheckInDate());
        target.setCheckOutDate(source.getCheckOutDate());
        target.setRoomNights(resolveRoomNights(source));
        target.setRoomStatus(source.getRoomStatus());
        target.setCorporateCode(source.getCorporateCode());
        target.setRoomType(source.getRoomType());
        target.setCompany(source.getCompany());
        target.setSharingStatus(source.getSharingStatus());
        target.setFloor(source.getFloor());
        target.setBalance(source.getBalance());
        target.setLoyaltyMembershipStatus(source.getLoyaltyMembershipStatus());
        target.setSourceLastSyncedAt(LocalDateTime.now());
    }

    public ArrivalResponseDto toResponse(ArrivalRecord source) {
        return ArrivalResponseDto.builder()
                .id(source.getId())
                .propertyId(source.getPropertyId())
                .status(source.getStatus())
                .salutation(source.getSalutation())
                .firstName(source.getFirstName())
                .lastName(source.getLastName())
                .roomNo(source.getRoomNo())
                .reservationType(source.getReservationType())
                .city(source.getCity())
                .rateCode(source.getRateCode())
                .checkInDate(source.getCheckInDate())
                .checkOutDate(source.getCheckOutDate())
                .roomNights(source.getRoomNights())
                .roomStatus(source.getRoomStatus())
                .corporateCode(source.getCorporateCode())
                .roomType(source.getRoomType())
                .confirmationNumber(source.getConfirmationNumber())
                .company(source.getCompany())
                .sharingStatus(source.getSharingStatus())
                .floor(source.getFloor())
                .balance(source.getBalance())
                .loyaltyMembershipStatus(source.getLoyaltyMembershipStatus())
                .build();
    }

    private Integer resolveRoomNights(ReservationArrivalDto source) {
        if (source.getRoomNights() != null) {
            return source.getRoomNights();
        }
        if (source.getCheckInDate() == null || source.getCheckOutDate() == null) {
            return 1;
        }
        long nights = ChronoUnit.DAYS.between(source.getCheckInDate(), source.getCheckOutDate());
        return (int) Math.max(1, nights);
    }
}

