package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

public interface ReservationPaymentTransactionRepository extends JpaRepository<ReservationPaymentTransactionRecord, Long> {

	List<ReservationPaymentTransactionRecord> findByBookingIdIn(List<Long> bookingIds);

	Optional<ReservationPaymentTransactionRecord> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

	@Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM ReservationPaymentTransactionRecord t
    WHERE t.confirmationNumber = :confirmationNumber
      AND UPPER(t.transactionStatus) = 'SUCCESS'
    """)
	java.math.BigDecimal sumSuccessfulPaymentsByConfirmationNumber(String confirmationNumber);
}
