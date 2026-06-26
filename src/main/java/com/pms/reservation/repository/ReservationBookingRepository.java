package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationBookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationBookingRepository extends JpaRepository<ReservationBookingRecord, Long> {
}
