package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationBookingRecord;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReservationBookingRepository
	extends JpaRepository<ReservationBookingRecord, Long>, JpaSpecificationExecutor<ReservationBookingRecord> {

	Optional<ReservationBookingRecord> findByConfirmationNumber(String confirmationNumber);

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
}
