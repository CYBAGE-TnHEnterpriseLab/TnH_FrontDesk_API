package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationCheckInIdProofRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCheckInIdProofRepository extends JpaRepository<ReservationCheckInIdProofRecord, Long> {

    Optional<ReservationCheckInIdProofRecord> findByBookingId(Long bookingId);
}