package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationCheckInSignatureRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCheckInSignatureRepository extends JpaRepository<ReservationCheckInSignatureRecord, Long> {

    Optional<ReservationCheckInSignatureRecord> findByBookingId(Long bookingId);
}
