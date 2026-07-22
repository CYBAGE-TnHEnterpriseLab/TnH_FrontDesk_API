package com.pms.housekeeping.repository;

import com.pms.housekeeping.entity.RoomMasterProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMasterProjectionRepository extends JpaRepository<RoomMasterProjection, Long> {

    List<RoomMasterProjection> findAllByPropertyId(UUID propertyId);

    Optional<RoomMasterProjection> findByPropertyIdAndRoomNumber(UUID propertyId, String roomNumber);
}


