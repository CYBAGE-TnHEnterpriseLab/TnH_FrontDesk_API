package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationBookingRecord;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

public interface ReservationBookingRepository
	extends JpaRepository<ReservationBookingRecord, Long>, JpaSpecificationExecutor<ReservationBookingRecord> {

    List<ReservationBookingRecord> findByConfirmationNumber(String confirmationNumber);

    Optional<ReservationBookingRecord> findByIdAndConfirmationNumber(Long id, String confirmationNumber);

    List<ReservationBookingRecord> findByConfirmationNumberOrderByIdAsc(String confirmationNumber);

	List<ReservationBookingRecord> findAllByOrderByCreatedAtDesc();

	List<ReservationBookingRecord> findByPropertyIdAndAssignedRoomNoIsNotNullAndArrivalDateLessThanAndDepartureDateGreaterThan(
		String propertyId,
		java.time.LocalDate arrivalDateUpperExclusive,
		java.time.LocalDate departureDateLowerExclusive
	);

	List<ReservationBookingRecord> findByPropertyIdAndConfirmationNumberIn(
		String propertyId,
		Collection<String> confirmationNumbers
	);

	boolean existsByConfirmationNumber(String confirmationNumber);

    @Modifying
    @Query("""
        UPDATE ReservationBookingRecord r
        SET r.reservationStatus = 'NO_SHOW'
        WHERE r.propertyId = :propertyId
          AND UPPER(r.reservationStatus) = 'CONFIRMED'
          AND r.arrivalDate < :businessDate
        """)
    int markPastConfirmedReservationsAsNoShow(
            @Param("propertyId") String propertyId,
            @Param("businessDate") LocalDate businessDate
    );

	@Query("""
    SELECT
        COALESCE(
            SUM(r.rate * r.numberOfRooms),
            0
        ) AS roomRevenue,

        COALESCE(
            SUM(r.numberOfRooms),
            0
        ) AS roomsSold,

        COALESCE(
            SUM(
                CASE
                    WHEN UPPER(r.reservationType) = 'GROUP'
                    THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS groupBookings,

        COALESCE(
            SUM(
                CASE
                    WHEN UPPER(r.reservationType) <> 'GROUP'
                    THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS individualBookings

    FROM ReservationBookingRecord r

    WHERE r.propertyId = :propertyId

      AND r.arrivalDate <= :businessDate

      AND r.departureDate > :businessDate

      AND UPPER(r.reservationStatus) NOT IN (
          'CANCELLED',
          'CANCELED',
          'NO_SHOW'
      )
    """)
	Optional<DailyRevenueProjection> findDailyRevenue(
			@Param("propertyId") String propertyId,
			@Param("businessDate") LocalDate businessDate
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.arrivalDate = :businessDate
      AND (r.source IS NULL OR TRIM(r.source) = '')
    """)
	long countWalkInsByPropertyIdAndArrivalDate(
			@Param("propertyId") String propertyId,
			@Param("businessDate") LocalDate businessDate
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.createdAt >= :businessDateStart
      AND r.createdAt < :businessDateEnd
    """)
	long countNewReservationsByPropertyIdAndBusinessDate(
			@Param("propertyId") String propertyId,
			@Param("businessDateStart") LocalDateTime businessDateStart,
			@Param("businessDateEnd") LocalDateTime businessDateEnd
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.checkOutCompletedAt IS NOT NULL
      AND r.checkOutCompletedAt >= :businessDateStart
      AND r.checkOutCompletedAt < :businessDateEnd
    """)
	long countCheckedOutsByPropertyIdAndBusinessDate(
			@Param("propertyId") String propertyId,
			@Param("businessDateStart") LocalDateTime businessDateStart,
			@Param("businessDateEnd") LocalDateTime businessDateEnd
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.checkInCompletedAt >= :businessDateStart
      AND r.checkInCompletedAt < :businessDateEnd
    """)
	long countCheckInsByPropertyIdAndBusinessDate(
			@Param("propertyId") String propertyId,
			@Param("businessDateStart") LocalDateTime businessDateStart,
			@Param("businessDateEnd") LocalDateTime businessDateEnd
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.checkOutCompletedAt IS NOT NULL
      AND r.checkOutCompletedAt >= :businessDateStart
      AND r.checkOutCompletedAt < :businessDateEnd
      AND r.checkOutCompletedAt < r.departureDate
    """)
	long countEarlyDeparturesByPropertyIdAndBusinessDate(
			@Param("propertyId") String propertyId,
			@Param("businessDateStart") LocalDateTime businessDateStart,
			@Param("businessDateEnd") LocalDateTime businessDateEnd
	);

	@Query("""
    SELECT COUNT(r)
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.reservationStatus IS NOT NULL
      AND UPPER(r.reservationStatus) IN ('CANCELLED', 'CANCELED')
      AND r.updatedAt >= :businessDateStart
      AND r.updatedAt < :businessDateEnd
    """)
	long countSameDayCancelsByPropertyIdAndBusinessDate(
			@Param("propertyId") String propertyId,
			@Param("businessDateStart") LocalDateTime businessDateStart,
			@Param("businessDateEnd") LocalDateTime businessDateEnd
	);

	@Query("""
    SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
    FROM ReservationBookingRecord r
    WHERE r.propertyId = :propertyId
      AND r.guestName = :guestName
      AND r.assignedRoomNo = :assignedRoomNo
      AND r.arrivalDate = :arrivalDate
      AND r.departureDate = :departureDate
      AND UPPER(r.reservationStatus) NOT IN ('CANCELLED', 'CANCELED', 'NO_SHOW')
    """)
	boolean existsActiveBookingForGuestRoomAndDates(
			@Param("propertyId") String propertyId,
			@Param("guestName") String guestName,
			@Param("assignedRoomNo") String assignedRoomNo,
			@Param("arrivalDate") LocalDate arrivalDate,
			@Param("departureDate") LocalDate departureDate
	);
}
