package com.pms.inventory.reservation.repository;

import com.pms.inventory.reservation.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByReservationId(UUID reservationId);
}

