package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationPaymentTransactionRepository extends JpaRepository<ReservationPaymentTransactionRecord, Long> {

	List<ReservationPaymentTransactionRecord> findByBookingIdIn(List<Long> bookingIds);

	Optional<ReservationPaymentTransactionRecord> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
