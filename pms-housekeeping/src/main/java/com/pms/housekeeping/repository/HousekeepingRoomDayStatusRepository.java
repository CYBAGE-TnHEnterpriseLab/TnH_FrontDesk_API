package com.pms.housekeeping.repository;

import com.pms.housekeeping.constant.QueryConstants;
import com.pms.housekeeping.dto.response.RoomTypeOptionResponse;
import com.pms.housekeeping.entity.CleaningStatus;
import com.pms.housekeeping.entity.FrontOfficeStatus;
import com.pms.housekeeping.entity.HousekeepingRoomDayStatus;
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
            String roomTypeId,
            List<CleaningStatus> cleaningStatuses,
            FrontOfficeStatus frontOfficeStatus);

    @Query(value = QueryConstants.FIND_DISTINCT_ROOM_TYPES)
    List<RoomTypeOptionResponse> findDistinctRoomTypes(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);

    @Query(value = QueryConstants.FIND_DISTINCT_FLOORS)
    List<String> findDistinctFloors(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);

    @Query(value = QueryConstants.FIND_DISTINCT_ATTENDANTS)
    List<String> findDistinctAttendants(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate);


    @Query(value = QueryConstants.FIND_CALENDAR_DATA)
    List<HousekeepingRoomDayStatus> findCalendarData(
            @Param("propertyId") String propertyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("roomTypes") List<String> roomTypes
    );

    @Modifying(clearAutomatically = true)
    @Query(value = QueryConstants.UPDATE_CLEANING_STATUS_FROM_DATE, nativeQuery = true)
    int updateCleaningStatusFromDate(
            @Param("propertyId") String propertyId,
            @Param("roomNumber") String roomNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") String status,
            @Param("lastCleanedAt") LocalDateTime lastCleanedAt,
            @Param("sellable") boolean sellable,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    @Modifying(clearAutomatically = true)
    @Query(value = QueryConstants.UPDATE_CLEANING_STATUS_FROM_DATE_AFTER_CHECKOUT, nativeQuery = true)
    int updateCleaningStatusFromDateAfterCheckout(
            @Param("propertyId") String propertyId,
            @Param("roomNumber") String roomNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") String status,
            @Param("lastCleanedAt") LocalDateTime lastCleanedAt,
            @Param("sellable") boolean sellable,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    void deleteByPropertyId(String propertyId);
}