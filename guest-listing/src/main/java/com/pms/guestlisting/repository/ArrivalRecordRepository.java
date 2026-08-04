package com.pms.guestlisting.repository;

import com.pms.guestlisting.entity.ArrivalRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ArrivalRecordRepository extends JpaRepository<ArrivalRecord, Long>, JpaSpecificationExecutor<ArrivalRecord> {

    Optional<ArrivalRecord> findByPropertyIdAndBusinessDateAndConfirmationNumber(
            String propertyId,
            LocalDate businessDate,
            String confirmationNumber
    );

    boolean existsByPropertyIdAndBusinessDate(String propertyId, LocalDate businessDate);

    @Query("select distinct a.status from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.status is not null and a.status <> '' order by a.status")
    List<String> findDistinctStatuses(String propertyId, LocalDate businessDate);

    @Query("select distinct a.reservationType from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.reservationType is not null and a.reservationType <> '' order by a.reservationType")
    List<String> findDistinctReservationTypes(String propertyId, LocalDate businessDate);

    @Query("select distinct a.city from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.city is not null and a.city <> '' order by a.city")
    List<String> findDistinctCities(String propertyId, LocalDate businessDate);

    @Query("select distinct a.roomStatus from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.roomStatus is not null and a.roomStatus <> '' order by a.roomStatus")
    List<String> findDistinctRoomStatuses(String propertyId, LocalDate businessDate);

    @Query("select distinct a.roomType from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.roomType is not null and a.roomType <> '' order by a.roomType")
    List<String> findDistinctRoomTypes(String propertyId, LocalDate businessDate);

    @Query("select distinct a.floor from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.floor is not null order by a.floor")
    List<Integer> findDistinctFloors(String propertyId, LocalDate businessDate);

    @Query("select distinct a.company from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.company is not null and a.company <> '' order by a.company")
    List<String> findDistinctCompanies(String propertyId, LocalDate businessDate);

    @Query("select distinct a.loyaltyMembershipStatus from ArrivalRecord a where a.propertyId = :propertyId and a.businessDate = :businessDate and a.checkInDate = :businessDate and a.loyaltyMembershipStatus is not null and a.loyaltyMembershipStatus <> '' order by a.loyaltyMembershipStatus")
    List<String> findDistinctLoyaltyMembershipStatuses(String propertyId, LocalDate businessDate);
}

