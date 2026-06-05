package com.hotel.pms.frontdesk.guestlisting.repository;

import com.hotel.pms.frontdesk.guestlisting.entity.DepartureRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface DepartureRecordRepository extends JpaRepository<DepartureRecord, Long>, JpaSpecificationExecutor<DepartureRecord> {

    Optional<DepartureRecord> findByPropertyIdAndBusinessDateAndConfirmationNumber(
            String propertyId,
            LocalDate businessDate,
            String confirmationNumber
    );

    boolean existsByPropertyIdAndBusinessDate(String propertyId, LocalDate businessDate);

    @Query("select distinct d.status from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.status is not null and d.status <> '' order by d.status")
    List<String> findDistinctStatuses(String propertyId, LocalDate businessDate);

    @Query("select distinct d.reservationType from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.reservationType is not null and d.reservationType <> '' order by d.reservationType")
    List<String> findDistinctReservationTypes(String propertyId, LocalDate businessDate);

    @Query("select distinct d.city from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.city is not null and d.city <> '' order by d.city")
    List<String> findDistinctCities(String propertyId, LocalDate businessDate);

    @Query("select distinct d.roomStatus from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.roomStatus is not null and d.roomStatus <> '' order by d.roomStatus")
    List<String> findDistinctRoomStatuses(String propertyId, LocalDate businessDate);

    @Query("select distinct d.roomType from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.roomType is not null and d.roomType <> '' order by d.roomType")
    List<String> findDistinctRoomTypes(String propertyId, LocalDate businessDate);

    @Query("select distinct d.floor from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.floor is not null order by d.floor")
    List<Integer> findDistinctFloors(String propertyId, LocalDate businessDate);

    @Query("select distinct d.company from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.company is not null and d.company <> '' order by d.company")
    List<String> findDistinctCompanies(String propertyId, LocalDate businessDate);

    @Query("select distinct d.loyaltyMembershipStatus from DepartureRecord d where d.propertyId = :propertyId and d.businessDate = :businessDate and d.checkOutDate = :businessDate and d.loyaltyMembershipStatus is not null and d.loyaltyMembershipStatus <> '' order by d.loyaltyMembershipStatus")
    List<String> findDistinctLoyaltyMembershipStatuses(String propertyId, LocalDate businessDate);
}
