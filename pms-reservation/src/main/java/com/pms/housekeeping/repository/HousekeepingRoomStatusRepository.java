package com.pms.housekeeping.repository;

import com.pms.housekeeping.entity.HousekeepingRoomStatusRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HousekeepingRoomStatusRepository extends JpaRepository<HousekeepingRoomStatusRecord, Long> {

    Optional<HousekeepingRoomStatusRecord> findByPropertyIdAndBusinessDateAndConfirmationNumber(
            String propertyId,
            LocalDate businessDate,
            String confirmationNumber
    );

    List<HousekeepingRoomStatusRecord> findByPropertyIdAndBusinessDateAndConfirmationNumberIn(
            String propertyId,
            LocalDate businessDate,
            Collection<String> confirmationNumbers
    );

    List<HousekeepingRoomStatusRecord> findByPropertyIdAndBusinessDateBetweenAndRoomNoIsNotNull(
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            select h.confirmationNumber
            from HousekeepingRoomStatusRecord h
            where h.propertyId = :propertyId
              and h.businessDate = :businessDate
              and lower(h.roomStatus) = lower(:roomStatus)
            """)
    List<String> findConfirmationNumbersByPropertyIdAndBusinessDateAndRoomStatus(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate,
            @Param("roomStatus") String roomStatus
    );

        void deleteByPropertyIdAndConfirmationNumber(String propertyId, String confirmationNumber);
}
