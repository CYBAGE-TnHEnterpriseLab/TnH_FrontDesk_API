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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HousekeepingRoomDayStatusRepository
        extends JpaRepository<HousekeepingRoomDayStatus, Long>,
        JpaSpecificationExecutor<HousekeepingRoomDayStatus> {

    List<HousekeepingRoomDayStatus> findAllByPropertyIdAndBusinessDate(
            UUID propertyId,
            LocalDate businessDate);

    Optional<HousekeepingRoomDayStatus> findByPropertyIdAndBusinessDateAndRoomNumber(
            UUID propertyId,
            LocalDate businessDate,
            String roomNumber);

    List<HousekeepingRoomDayStatus> findTop200ByPropertyIdAndBusinessDateAndRoomTypeIdAndSellableTrueAndAssignedReservationIdIsNullAndCleaningStatusInAndFrontOfficeStatusOrderByRoomNumberAsc(
            UUID propertyId,
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
            @Param("propertyId") UUID propertyId,
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
            @Param("propertyId") UUID propertyId,
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
            @Param("propertyId") UUID propertyId,
            @Param("businessDate") LocalDate businessDate);
}