package com.pms.reservation.repository;

import com.pms.reservation.entity.ReservationGuest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, Long> {

    List<ReservationGuest> findByBookingId(Long bookingId);

    Optional<ReservationGuest> findByBookingIdAndIsPrimaryTrue(Long bookingId);

    Optional<ReservationGuest> findByBookingIdAndGuestProfileId(Long bookingId, Long guestProfileId);

    Optional<ReservationGuest> findByIdAndBookingId(Long id, Long bookingId);

    List<ReservationGuest> findByGuestProfileId(Long guestProfileId);

    boolean existsByBookingIdAndGuestProfileId(Long bookingId, Long guestProfileId);
}
