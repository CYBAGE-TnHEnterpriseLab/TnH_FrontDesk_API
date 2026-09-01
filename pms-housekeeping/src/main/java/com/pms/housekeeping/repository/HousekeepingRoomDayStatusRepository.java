package com.pms.housekeeping.repository;

import com.pms.housekeeping.dto.response.RoomTypeOptionResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HousekeepingRoomDayStatusRepository
        extends JpaRepository<HousekeepingRoomDayStatus, Long>,
        JpaSpecificationExecutor<HousekeepingRoomDayStatus> {

    List<HousekeepingRoomDayStatus> findAllByPropertyIdAndBusinessDate(
            String propertyId,
            LocalDate businessDate);

    List<HousekeepingRoomDayStatus> findAllByPropertyIdAndBusinessDateBetween(
            String propertyId,
            LocalDate fromDate,
            LocalDate toDate);

    Optional<HousekeepingRoomDayStatus> findByPropertyIdAndBusinessDateAndRoomNumber(
            String propertyId,
            LocalDate businessDate,
            String roomNumber);

    List<HousekeepingRoomDayStatus> findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndConfirmationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
            String propertyId,
            LocalDate businessDate,
            UUID roomTypeId,
            List<CleaningStatus> cleaningStatuses,
            FrontOfficeStatus frontOfficeStatus);

    @Query("""
select distinct new com.pms.housekeeping.dto.response.RoomTypeOptionResponse(
       h.roomTypeId,
       h.roomTypeName
)
from HousekeepingRoomDayStatus h
where h.propertyId = :propertyId
and h.businessDate = :businessDate
order by h.roomTypeName
""")
    List<RoomTypeOptionResponse> findDistinctRoomTypes(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);

    @Query("""
        select distinct h.floor
        from HousekeepingRoomDayStatus h
        where h.propertyId=:propertyId
        and h.businessDate=:businessDate
        and h.floor is not null
        and trim(h.floor) <> ''
        order by h.floor
        """)
    List<String> findDistinctFloors(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);

    @Query("""
    select distinct h.attendantName
    from HousekeepingRoomDayStatus h
    where h.propertyId = :propertyId
      and h.businessDate = :businessDate
      and h.attendantName is not null
    order by h.attendantName
    """)
    List<String> findDistinctAttendants(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);


    //Calender data query
    @Query("""
    SELECT r
    FROM HousekeepingRoomDayStatus r
    WHERE r.propertyId = :propertyId
      AND r.businessDate BETWEEN :fromDate AND :toDate
      AND (
            :roomTypes IS NULL
            OR r.roomTypeName IN :roomTypes
      )
    ORDER BY r.roomTypeName, r.roomNumber, r.businessDate
    """)
    List<HousekeepingRoomDayStatus> findCalendarData(
            @Param("propertyId") String propertyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("roomTypes") List<String> roomTypes
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE HousekeepingRoomDayStatus r
       SET r.cleaningStatus = :status,
           r.lastCleanedAt = :lastCleanedAt,
           r.updatedAt = :updatedAt,
           r.updatedBy = :updatedBy
     WHERE r.propertyId = :propertyId
       AND r.roomNumber = :roomNumber
       AND r.businessDate >= :fromDate
""")
    int updateCleaningStatusFromDate(
            @Param("propertyId") String propertyId,
            @Param("roomNumber") String roomNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") CleaningStatus status,
            @Param("lastCleanedAt") LocalDateTime lastCleanedAt,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE HousekeepingRoomDayStatus r
       SET r.cleaningStatus = :status,
           r.lastCleanedAt = null,
           r.updatedAt = :updatedAt,
           r.updatedBy = :updatedBy
     WHERE r.propertyId = :propertyId
       AND r.roomNumber = :roomNumber
       AND r.businessDate >= :fromDate
""")
    int updateCleaningStatusFromDateAfterCheckout(
            @Param("propertyId") String propertyId,
            @Param("roomNumber") String roomNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") CleaningStatus status,
            @Param("lastCleanedAt") LocalDateTime lastCleanedAt,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    void deleteByPropertyId(String propertyId);
}