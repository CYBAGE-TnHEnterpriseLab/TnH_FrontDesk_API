package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationCheckInWorkflowRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCheckInWorkflowRepository extends JpaRepository<ReservationCheckInWorkflowRecord, Long> {

    Optional<ReservationCheckInWorkflowRecord> findByBookingId(Long bookingId);
}
