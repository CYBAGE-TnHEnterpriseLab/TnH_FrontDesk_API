package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationCheckInIdProofRecord;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCheckInIdProofRepository extends JpaRepository<ReservationCheckInIdProofRecord, Long> {

    Optional<ReservationCheckInIdProofRecord> findByBookingId(Long bookingId);

    List<ReservationCheckInIdProofRecord> findAllByBookingIdIn(Collection<Long> bookingIds);
}