package com.pms.inventory.reservation.repository;

import com.pms.inventory.reservation.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByConfirmationNumber(String confirmationNumber);
}

