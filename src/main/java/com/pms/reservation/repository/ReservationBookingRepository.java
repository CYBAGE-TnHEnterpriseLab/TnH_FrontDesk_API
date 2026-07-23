package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationBookingRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReservationBookingRepository
	extends JpaRepository<ReservationBookingRecord, Long>, JpaSpecificationExecutor<ReservationBookingRecord> {

	Optional<ReservationBookingRecord> findByConfirmationNumber(String confirmationNumber);

	boolean existsByConfirmationNumber(String confirmationNumber);
}
