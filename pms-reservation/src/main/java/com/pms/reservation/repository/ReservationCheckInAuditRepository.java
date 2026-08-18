package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationCheckInAuditRecord;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationCheckInAuditRepository extends JpaRepository<ReservationCheckInAuditRecord, Long> {

    List<ReservationCheckInAuditRecord> findByBookingIdOrderByCreatedAtAsc(Long bookingId);

    Page<ReservationCheckInAuditRecord> findByBookingId(Long bookingId, Pageable pageable);

    Page<ReservationCheckInAuditRecord> findByBookingIdAndEventTypeIgnoreCase(
        Long bookingId,
        String eventType,
        Pageable pageable
    );

    Page<ReservationCheckInAuditRecord> findByBookingIdAndCreatedAtBetween(
        Long bookingId,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    Page<ReservationCheckInAuditRecord> findByBookingIdAndEventTypeIgnoreCaseAndCreatedAtBetween(
        Long bookingId,
        String eventType,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
}
