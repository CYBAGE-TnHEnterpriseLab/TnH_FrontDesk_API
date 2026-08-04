package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationPaymentTransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationPaymentTransactionRepository extends JpaRepository<ReservationPaymentTransactionRecord, Long> {
}
